package org.apache.hugegraph.loader.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;

public class MeTTaWriter {
    
    private static final String OUTPUT_DIR = "/home/developer/Desktop/metta_output";
    
    public MeTTaWriter() {
        // Create output directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory", e);
        }
    }
    
    /**
     * Write a list of vertices to MeTTa files, one file per label
     */
    public void writeNodes(List<Record> records) {
        Map<String, List<Vertex>> verticesByLabel = groupVerticesByLabel(records);
        
        for (Map.Entry<String, List<Vertex>> entry : verticesByLabel.entrySet()) {
            String label = entry.getKey();
            List<Vertex> vertices = entry.getValue();
            
            String filePath = OUTPUT_DIR + "/" + label +"_nodes"+ ".metta";
            
            try (FileWriter writer = new FileWriter(filePath, true)) {
                for (Vertex vertex : vertices) {
                    String mettaNode = writeNode(vertex);
                    writer.write(mettaNode + "\n");
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new RuntimeException("Failed to write nodes to file: " + filePath, e);
            }
        }
    }
    
    /**
     * Write a single vertex to MeTTa format
     */
    public String writeNode(Vertex vertex) {
        String id = vertex.id().toString();
        String label = vertex.label();
        Map<String, Object> properties = vertex.properties();
        
        StringBuilder result = new StringBuilder();
        result.append("(").append(label).append(" ").append(id).append(")");
        
        // Add properties
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            
            if (value != null && !value.toString().isEmpty()) {
                result.append("\n(").append(key).append(" (").append(label).append(" ").append(id).append(") ");
                
                // Handle different property types
                if (value instanceof List) {
                    result.append("(");
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        result.append(checkProperty(list.get(i).toString()));
                        if (i < list.size() - 1) {
                            result.append(" ");
                        }
                    }
                    result.append(")");
                } else {
                    result.append(checkProperty(value.toString()));
                }
                
                result.append(")");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Write a list of edges to MeTTa files, one file per label
     */
    public void writeEdges(List<Record> records) {
        Map<String, List<Edge>> edgesByLabel = groupEdgesByLabel(records);
        
        for (Map.Entry<String, List<Edge>> entry : edgesByLabel.entrySet()) {
            String label = entry.getKey();
            List<Edge> edges = entry.getValue();
            
            String filePath = OUTPUT_DIR + "/" + label +"_edges"+ ".metta";
            
            try (FileWriter writer = new FileWriter(filePath, true)) {
                for (Edge edge : edges) {
                    String mettaEdge = writeEdge(edge);
                    writer.write(mettaEdge + "\n");
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new RuntimeException("Failed to write edges to file: " + filePath, e);
            }
        }
    }
    
    /**
     * Write a single edge to MeTTa format
     */
    public String writeEdge(Edge edge) {
        String sourceId = edge.sourceId().toString();
        String targetId = edge.targetId().toString();
        String sourceLabel = edge.sourceLabel();
        String targetLabel = edge.targetLabel();
        String label = edge.label();
        Map<String, Object> properties = edge.properties();
        
        StringBuilder result = new StringBuilder();
        result.append("(").append(label).append(" (").append(sourceLabel).append(" ").append(sourceId).append(") ")
              .append("(").append(targetLabel).append(" ").append(targetId).append("))");
        
        // Add properties
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            
            if (value != null && !value.toString().isEmpty()) {
                result.append("\n(").append(key).append(" (").append(label)
                      .append(" (").append(sourceLabel).append(" ").append(sourceId).append(") ")
                      .append("(").append(targetLabel).append(" ").append(targetId).append(")) ");
                
                // Handle different property types
                if (value instanceof List) {
                    result.append("(");
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        result.append(checkProperty(list.get(i).toString()));
                        if (i < list.size() - 1) {
                            result.append(" ");
                        }
                    }
                    result.append(")");
                } else {
                    result.append(checkProperty(value.toString()));
                }
                
                result.append(")");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Helper method to escape special characters in property values
     */
    private String checkProperty(String prop) {
        if (prop.contains(" ")) {
            prop = prop.replace(" ", "_");
        }
        
        // Escape special characters
        String[] specialChars = {"(", ")"};
        String escapeChar = "\\";
        
        for (String special : specialChars) {
            prop = prop.replace(special, escapeChar + special);
        }
        
        return prop;
    }
    
    /**
     * Group vertices by their label
     */
    private Map<String, List<Vertex>> groupVerticesByLabel(List<Record> records) {
        Map<String, List<Vertex>> result = new java.util.HashMap<>();
        
        for (Record record : records) {
            Vertex vertex = (Vertex) record.element();
            String label = vertex.label();
            
            if (!result.containsKey(label)) {
                result.put(label, new ArrayList<>());
            }
            
            result.get(label).add(vertex);
        }
        
        return result;
    }
    
    /**
     * Group edges by their label
     */
    private Map<String, List<Edge>> groupEdgesByLabel(List<Record> records) {
        Map<String, List<Edge>> result = new java.util.HashMap<>();
        
        for (Record record : records) {
            Edge edge = (Edge) record.element();
            String label = edge.label();
            
            if (!result.containsKey(label)) {
                result.put(label, new ArrayList<>());
            }
            
            result.get(label).add(edge);
        }
        
        return result;
    }
}
