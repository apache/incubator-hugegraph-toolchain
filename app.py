from fastapi import FastAPI, File, UploadFile, Form, HTTPException, Depends
from fastapi.responses import JSONResponse, FileResponse, StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, List, Any, Optional, Union
import json
import shutil
import tempfile
import subprocess
import os
import uuid
import zipfile
import io

app = FastAPI(title="HugeGraph Loader API")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration for HugeGraph loader
HUGEGRAPH_LOADER_PATH = "./hugegraph-loader/apache-hugegraph-loader-incubating-1.5.0/bin/hugegraph-loader.sh"
HUGEGRAPH_HOST = "localhost"
HUGEGRAPH_PORT = "8080"
HUGEGRAPH_GRAPH = "hugegraph"
BASE_OUTPUT_DIR = os.path.abspath("./output")


# Schema Models
class PropertyKey(BaseModel):
    name: str
    type: str  # text, int, double, etc.
    # Optional properties
    cardinality: Optional[str] = None  # single, list, set
    # Other optional properties
    options: Optional[Dict[str, Any]] = None

class VertexLabel(BaseModel):
    name: str
    properties: List[str]
    primary_keys: Optional[List[str]] = None
    nullable_keys: Optional[List[str]] = None
    id_strategy: Optional[str] = None  # primary_key, automatic, customize_number, customize_string
    # Other optional properties
    options: Optional[Dict[str, Any]] = None

class EdgeLabel(BaseModel):
    name: str
    source_label: str
    target_label: str
    properties: Optional[List[str]] = None
    sort_keys: Optional[List[str]] = None
    # Other optional properties
    options: Optional[Dict[str, Any]] = None

class SchemaDefinition(BaseModel):
    property_keys: List[PropertyKey]
    vertex_labels: List[VertexLabel]
    edge_labels: List[EdgeLabel]

class HugeGraphLoadResponse(BaseModel):
    job_id: str
    status: str
    message: str
    details: Optional[Dict[str, Any]] = None
    # schema_groovy: Optional[str] = None
    output_files: Optional[List[str]] = None
    output_dir: Optional[str] = None
    schema_path: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None


def json_to_groovy(schema_json: Union[Dict, SchemaDefinition]) -> str:
    """
    Convert JSON schema definition to HugeGraph Groovy schema format.
    
    Args:
        schema_json: The schema definition in JSON format or as a SchemaDefinition object
        
    Returns:
        The equivalent schema in Groovy format
    """
    if isinstance(schema_json, dict):
        schema = SchemaDefinition(**schema_json)
    else:
        schema = schema_json
    
    groovy_lines = []
    
    # Process property keys
    for prop in schema.property_keys:
        line = f'schema.propertyKey("{prop.name}").as{prop.type.capitalize()}()'
        
        if prop.cardinality:
            line += f'.cardinality("{prop.cardinality}")'
        
        # Add any additional options from the options dictionary
        if prop.options:
            for opt_name, opt_value in prop.options.items():
                if isinstance(opt_value, str):
                    line += f'.{opt_name}("{opt_value}")'
                else:
                    line += f'.{opt_name}({opt_value})'
        
        # line += '.create();'
        line += '.ifNotExist().create();'
        groovy_lines.append(line)
    
    groovy_lines.append("")  # Empty line for readability
    
    # Process vertex labels
    for vertex in schema.vertex_labels:
        line = f'schema.vertexLabel("{vertex.name}")'
        
        # Add ID strategy if defined
        if vertex.id_strategy:
            if vertex.id_strategy == "primary_key":
                line += '.useCustomizeStringId()'  # This will be overridden by primaryKeys
            elif vertex.id_strategy == "customize_number":
                line += '.useCustomizeNumberId()'
            elif vertex.id_strategy == "customize_string":
                line += '.useCustomizeStringId()'
            elif vertex.id_strategy == "automatic":
                line += '.useAutomaticId()'
        
        # Add properties
        if vertex.properties:
            props_str = ', '.join([f'"{prop}"' for prop in vertex.properties])
            line += f'.properties({props_str})'
        
        # Add primary keys
        if vertex.primary_keys:
            keys_str = ', '.join([f'"{key}"' for key in vertex.primary_keys])
            line += f'.primaryKeys({keys_str})'
        
        # Add nullable keys
        if vertex.nullable_keys:
            keys_str = ', '.join([f'"{key}"' for key in vertex.nullable_keys])
            line += f'.nullableKeys({keys_str})'
        
        # Add any additional options
        if vertex.options:
            for opt_name, opt_value in vertex.options.items():
                if isinstance(opt_value, str):
                    line += f'.{opt_name}("{opt_value}")'
                else:
                    line += f'.{opt_name}({opt_value})'
        
        # line += '.create();'
        line += '.ifNotExist().create();'
        groovy_lines.append(line)
    
    groovy_lines.append("")  # Empty line for readability
    
    # Process edge labels
    for edge in schema.edge_labels:
        line = f'schema.edgeLabel("{edge.name}")'
        line += f'.sourceLabel("{edge.source_label}")'
        line += f'.targetLabel("{edge.target_label}")'
        
        # Add properties
        if edge.properties:
            props_str = ', '.join([f'"{prop}"' for prop in edge.properties])
            line += f'.properties({props_str})'
        
        # Add sort keys
        if edge.sort_keys:
            keys_str = ', '.join([f'"{key}"' for key in edge.sort_keys])
            line += f'.sortKeys({keys_str})'
        
        # Add any additional options
        if edge.options:
            for opt_name, opt_value in edge.options.items():
                if isinstance(opt_value, str):
                    line += f'.{opt_name}("{opt_value}")'
                else:
                    line += f'.{opt_name}({opt_value})'
        
        # line += '.create();'
        line += '.ifNotExist().create();'
        groovy_lines.append(line)
    
    x= '\n'.join(groovy_lines)
    print(x)
    return x


def update_file_paths_in_config(config, file_mapping):
    """
    Update file paths in the config to use the temporary file paths.
    
    Args:
        config: The configuration dictionary
        file_mapping: Dictionary mapping original filenames to temp file paths
    
    Returns:
        Updated configuration dictionary
    """
    # Create a deep copy of the config to avoid modifying the original
    updated_config = json.loads(json.dumps(config))
    
    # Function to process vertices and edges
    def update_paths(items):
        for item in items:
            if "input" in item and item["input"].get("type") == "file":
                # Extract just the filename from the path
                original_path = item["input"]["path"]
                filename = os.path.basename(original_path)
                
                # Check if this filename exists in our mapping
                if filename in file_mapping:
                    item["input"]["path"] = file_mapping[filename]
                # Otherwise, keep the original path (might be a relative path or a URL)
        
        return items
    
    # Update paths in vertices and edges
    if "vertices" in updated_config:
        updated_config["vertices"] = update_paths(updated_config["vertices"])
    
    if "edges" in updated_config:
        updated_config["edges"] = update_paths(updated_config["edges"])
    
    return updated_config


def get_job_output_dir(job_id):
    """
    Get a job-specific output directory
    
    Args:
        job_id: Unique job identifier
        
    Returns:
        Path to the output directory
    """
    output_dir = os.path.join(BASE_OUTPUT_DIR, job_id)
    # os.makedirs(output_dir, exist_ok=True)
    return output_dir


def get_output_files(output_dir):
    """
    Get list of output files in the specified directory
    
    Args:
        output_dir: Path to output directory
        
    Returns:
        List of file paths
    """
    if not os.path.exists(output_dir):
        return []
        
    files = []
    for filename in os.listdir(output_dir):
        file_path = os.path.join(output_dir, filename)
        if os.path.isfile(file_path):
            files.append(file_path)
    
    return files


def create_zip_file(files):
    """
    Create an in-memory zip file containing the output files
    
    Args:
        files: List of file paths to include in the zip
        
    Returns:
        In-memory zip file as bytes
    """
    memory_file = io.BytesIO()
    
    with zipfile.ZipFile(memory_file, 'w') as zf:
        for file_path in files:
            # Get the relative filename
            arcname = os.path.basename(file_path)
            zf.write(file_path, arcname=arcname)
    
    memory_file.seek(0)
    return memory_file

def load_metadata(output_dir):
    metadata_file = os.path.join(output_dir, "graph_metadata.json")
    metadata = {}  # Default empty dictionary
    
    try:
        with open(metadata_file, 'r') as f:
            metadata = json.load(f)
    except FileNotFoundError:
        print(f"Metadata file not found: {metadata_file}")
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON in metadata file: {e}")
    except Exception as e:
        print(f"Unexpected error loading metadata: {e}")
    
    return metadata

@app.post("/api/load", response_model=HugeGraphLoadResponse)
async def load_data(
    files: List[UploadFile] = File(...),
    config: str = Form(...),
    schema_json: Optional[str] = Form(None),
):
    """
    API endpoint to load data into HugeGraph.
    
    - files: Multiple data files to be loaded
    - config: JSON configuration similar to struct.json
    - schema_json: Optional schema in JSON format that will be converted to Groovy
    """
    # Generate a unique job ID
    job_id = str(uuid.uuid4())
    
    try:
        # Parse the config
        config_data = json.loads(config)
        
        # Create a temporary directory for this job
        with tempfile.TemporaryDirectory() as tmpdir:
            # Dictionary to map original filenames to their paths in the temp directory
            file_mapping = {}
            
            # Save all uploaded files to temp directory
            for file in files:
                file_path = os.path.join(tmpdir, file.filename)
                with open(file_path, "wb") as f:
                    shutil.copyfileobj(file.file, f)
                file_mapping[file.filename] = file_path
            
            # Convert JSON schema to Groovy if provided
            schema_path = None
            schema_groovy = None
            if schema_json:
                schema_data = json.loads(schema_json)
                schema_groovy = json_to_groovy(schema_data)
                schema_path = os.path.join(tmpdir, f"schema-{job_id}.groovy")
                with open(schema_path, "w") as f:
                    f.write(schema_groovy)
            else:
                raise HTTPException(status_code=400, detail="Schema JSON is required")
            
            # Update the paths in the config to point to the temp files
            updated_config = update_file_paths_in_config(config_data, file_mapping)
            
            # Save the updated config to a file
            config_path = os.path.join(tmpdir, f"struct-{job_id}.json")
            with open(config_path, "w") as f:
                json.dump(updated_config, f, indent=2)
            
            # Get job-specific output directory
            output_dir = get_job_output_dir(job_id)
            
            # Build the HugeGraph loader command
            cmd = [
                "sh", HUGEGRAPH_LOADER_PATH,
                "-g", HUGEGRAPH_GRAPH,
                "-f", config_path,
                "-h", HUGEGRAPH_HOST,
                "-p", HUGEGRAPH_PORT,
                "--clear-all-data", "true",
                "-o", output_dir  # Specify output directory
            ]
            
            if schema_path:
                cmd.extend(["-s", schema_path])
            
            # Run the command
            result = subprocess.run(cmd, capture_output=True, text=True)
            
            # Get output files
            output_files = get_output_files(output_dir)
            output_filenames = [os.path.basename(f) for f in output_files]
            metadata = load_metadata(output_dir)

            if result.returncode != 0:
                return HugeGraphLoadResponse(
                    job_id=job_id,
                    status="error",
                    message=f"HugeGraph loader failed with exit code {result.returncode}",
                    details={
                        "stdout": result.stdout,
                        "stderr": result.stderr
                    },
                    output_files=output_filenames
                )
            if len(output_files) == 0:
                return HugeGraphLoadResponse(
                    job_id=job_id,
                    status="error",
                    message="No output files generated",
                    details={
                        "stdout": result.stdout,
                        "stderr": result.stderr
                    },
                )
            
            # save the schema json to the output directory
            schema_json_path = os.path.join(output_dir, f"schema.json")
            with open(schema_json_path, "w") as f:
                json.dump(schema_data, f, indent=2)
            output_filenames.append(os.path.basename(schema_json_path))

            return HugeGraphLoadResponse(
                job_id=job_id,
                status="success",
                message="Graph generated successfully",
                metadata=metadata,
                details={
                    "stdout": result.stdout
                },
                output_files=output_filenames,
                output_dir=output_dir,
                schema_path=schema_json_path
            )
            
    except json.JSONDecodeError as e:
        raise HTTPException(status_code=400, detail=f"Invalid JSON: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing request: {str(e)}")


@app.get("/api/output/{job_id}")
async def get_output(job_id: str):
    """
    Retrieve output files for a specific job
    
    Args:
        job_id: The job ID to retrieve output files for
        
    Returns:
        Zip file containing all output files
    """
    output_dir = os.path.join(BASE_OUTPUT_DIR, job_id)
    
    if not os.path.exists(output_dir):
        raise HTTPException(status_code=404, detail=f"No output files found for job ID: {job_id}")
    
    files = get_output_files(output_dir)
    
    if not files:
        raise HTTPException(status_code=404, detail=f"No output files found for job ID: {job_id}")
    
    # Create a zip file with all output files
    zip_bytes = create_zip_file(files)
    
    return StreamingResponse(
        zip_bytes, 
        media_type="application/zip",
        headers={"Content-Disposition": f"attachment; filename=output-{job_id}.zip"}
    )


@app.get("/api/output-file/{job_id}/{filename}")
async def get_output_file(job_id: str, filename: str):
    """
    Retrieve a specific output file for a job
    
    Args:
        job_id: The job ID
        filename: The filename to retrieve
        
    Returns:
        The requested file
    """
    file_path = os.path.join(BASE_OUTPUT_DIR, job_id, filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail=f"File not found: {filename} for job ID: {job_id}")
    
    return FileResponse(
        file_path,
        headers={"Content-Disposition": f"attachment; filename={filename}"}
    )


@app.post("/api/convert-schema", response_class=JSONResponse)
async def convert_schema(schema_json: Dict = None):
    """
    Convert JSON schema to Groovy format without loading data.
    
    Args:
        schema_json: The schema definition in JSON format
        
    Returns:
        The equivalent schema in Groovy format
    """
    try:
        groovy_schema = json_to_groovy(schema_json)
        return {
            "status": "success",
            "schema_groovy": groovy_schema
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Error converting schema: {str(e)}")


@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)