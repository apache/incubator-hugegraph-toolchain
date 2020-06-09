import isInt from 'validator/lib/isInt';
import isFloat from 'validator/lib/isFloat';
import isBoolean from 'validator/lib/isBoolean';
import isISO8601 from 'validator/lib/isISO8601';
import isUUID from 'validator/lib/isUUID';
import isEmpty from 'validator/lib/isEmpty';

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

export function convertArrayToString(
  values: any[] | any,
  separtor: string = ','
) {
  return Array.isArray(values)
    ? values.filter((value) => value !== '').join(separtor)
    : String(values);
}

export function validateGraphProperty(
  type: string,
  value: string,
  premitEmpty: boolean = false
) {
  if (premitEmpty) {
    if (value === '') {
      return true;
    }
  }

  switch (type) {
    case 'BOOLEAN':
      return isBoolean(value);
    case 'BYTE':
      return isInt(value) && Number(value) > -128 && Number(value) <= 127;
    case 'INT':
    case 'LONG':
      return isInt(value);
    case 'FLOAT':
    case 'DOUBLE':
      return isFloat(value);
    case 'TEXT':
    case 'BLOB':
      return !isEmpty(value);
    case 'DATE':
      return isISO8601(value);
    case 'UUID':
      return isUUID(value);
  }
}
