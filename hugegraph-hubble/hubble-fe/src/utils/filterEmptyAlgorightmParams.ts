import { omitBy, cloneDeep, isEmpty } from 'lodash-es';

export function removeLabelKey<T extends Record<string, any>>(
  params: T,
  isCloned = true,
  key = 'label'
) {
  const paramObject = isCloned ? params : cloneDeep(params);

  if (paramObject[key] === '__all__') {
    delete paramObject[key];
  }

  return paramObject;
}

export function filterEmptyAlgorightmParams(params: object, keys: string[]) {
  return omitBy(params, (value, key) => keys.includes(key) && isEmpty(value));
}
