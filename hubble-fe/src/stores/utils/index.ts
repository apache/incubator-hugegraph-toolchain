import {
  VertexTypeProperty,
  MetadataProperty
} from '../types/GraphManagementStore/metadataConfigsStore';

export function checkIfLocalNetworkOffline(error: any) {
  if (!error.response) {
    throw new Error('网络错误，请查看您的本地连接');
  }
}

export function mapMetadataProperties(
  properties: VertexTypeProperty[],
  propertyCollection: MetadataProperty[]
) {
  const mappedProperties: Record<string, string> = {};

  properties.forEach(({ name }) => {
    const value = propertyCollection.find(
      ({ name: propertyName }) => propertyName === name
    )!.data_type;

    mappedProperties[name] = value;
  });

  return mappedProperties;
}

export function generateGraphModeId(
  id: string,
  source: string,
  target: string
): string {
  return `${source}-${id}->${target}`;
}
