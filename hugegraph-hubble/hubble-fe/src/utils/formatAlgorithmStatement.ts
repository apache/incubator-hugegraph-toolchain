import {
  isUndefined,
  cloneDeep,
  trimEnd,
  isEmpty,
  isObject,
  fromPairs
} from 'lodash-es';

import type { TFunction, i18n } from 'i18next';
import type {
  NeighborRankParams,
  CustomPathParams
} from '../stores/types/GraphManagementStore/dataAnalyzeStore';

export const AlgorithmInternalNameMapping: Record<string, string> = {
  rings: 'loop-detection',
  crosspoints: 'focus-detection',
  shortpath: 'shortest-path',
  allshortpath: 'shortest-path-all',
  paths: 'all-path',
  fsimilarity: 'model-similarity',
  neighborrank: 'neighbor-rank',
  kneighbor: 'k-step-neighbor',
  kout: 'k-hop',
  customizedpaths: 'custom-path',
  rays: 'radiographic-inspection',
  sameneighbors: 'same-neighbor',
  weightedshortpath: 'weighted-shortest-path',
  singleshortpath: 'single-source-weighted-shortest-path',
  jaccardsimilarity: 'jaccard',
  personalrank: 'personal-rank'
};

export function formatAlgorithmStatement(
  content: string,
  algorithmType: string | undefined,
  translator: TFunction,
  i18n: i18n
) {
  if (isUndefined(algorithmType)) {
    return [''];
  }

  const algorithmName = translator(
    `data-analyze.algorithm-forms.api-name-mapping.${algorithmType}`
  );
  let algorithmParams = JSON.parse(content);
  const statements: string[] = [algorithmName];

  if (algorithmType === 'fsimilarity') {
    let convertedParams: Record<string, string>;

    if (!isUndefined(algorithmParams.sources)) {
      convertedParams = {
        source: algorithmParams.sources.ids ?? [],
        'vertex-type': algorithmParams.sources.label ?? '',
        'vertex-property': algorithmParams.sources.properties ?? [],
        direction: algorithmParams.direction,
        least_neighbor: algorithmParams.min_neighbors,
        similarity: algorithmParams.alpha,
        label:
          algorithmParams.label === null || algorithmParams.label === '__all__'
            ? translator(
                `data-analyze.algorithm-forms.model-similarity.pre-value`
              )
            : algorithmParams.label,
        max_similar: algorithmParams.top,
        least_similar: algorithmParams.min_similars,
        property_filter: algorithmParams.group_property,
        least_property_number: algorithmParams.min_groups,
        max_degree: algorithmParams.max_degree,
        capacity: algorithmParams.capacity,
        limit: algorithmParams.limit,
        return_common_connection: algorithmParams.with_intermediary,
        return_complete_info: algorithmParams.with_vertex
      };
    } else {
      // no need to convert relative fields in temp log (which sources field is undefined)
      convertedParams = { ...algorithmParams };
      convertedParams['vertex-type'] = convertedParams.vertexType;
      convertedParams['vertex-property'] = convertedParams.vertexProperty;
      convertedParams.label =
        algorithmParams.label === null || algorithmParams.label === '__all__'
          ? translator(
              `data-analyze.algorithm-forms.model-similarity.pre-value`
            )
          : algorithmParams.label;

      delete convertedParams.vertexType;
      delete convertedParams.vertexProperty;
    }

    algorithmParams = convertedParams;
  }

  if (algorithmType === 'neighborrank') {
    const convertedParams = cloneDeep(algorithmParams);

    convertedParams.steps = [];

    (algorithmParams as NeighborRankParams).steps.forEach(
      ({ degree, direction, labels, top }, stepIndex) => {
        const step: Record<string, any> = {};

        step[
          trimEnd(
            translator(
              'data-analyze.algorithm-forms.neighbor-rank.options.degree'
            ),
            ':'
          )
        ] = degree;
        step[
          trimEnd(
            translator(
              'data-analyze.algorithm-forms.neighbor-rank.options.direction'
            ),
            ':'
          )
        ] = direction;
        step[
          trimEnd(
            translator(
              'data-analyze.algorithm-forms.neighbor-rank.options.label'
            ),
            ':'
          )
        ] = isEmpty(labels)
          ? translator('data-analyze.algorithm-forms.neighbor-rank.pre-value')
          : labels.map((label) =>
              // value may be "__all__" from temp log (local)
              label === '__all__'
                ? translator(
                    'data-analyze.algorithm-forms.neighbor-rank.pre-value'
                  )
                : label
            );
        step[
          trimEnd(
            translator(
              'data-analyze.algorithm-forms.neighbor-rank.options.top'
            ),
            ':'
          )
        ] = top;

        convertedParams.steps[stepIndex] = step;
      }
    );

    algorithmParams = convertedParams;
  }

  // params struct fetched from server is not as consistent as front end
  if (algorithmType === 'kout' && algorithmParams.hasOwnProperty('step')) {
    algorithmParams.direction = algorithmParams.step.direction;
    algorithmParams.max_degree = algorithmParams.step.degree;
    algorithmParams.skip_degree = algorithmParams.step.skip_degree;
    algorithmParams.label = algorithmParams.step.labels[0] ?? null;

    delete algorithmParams.step;
  }

  if (algorithmType === 'customizedpaths') {
    let convertedParams: Record<string, any>;

    if (!isUndefined(algorithmParams.sources)) {
      convertedParams = {
        source: algorithmParams.sources.ids ?? [],
        'vertex-type': algorithmParams.sources.label ?? '',
        'vertex-property': algorithmParams.sources.properties ?? [],
        capacity: algorithmParams.capacity,
        limit: algorithmParams.limit,
        sort_by: algorithmParams.sort_by,
        steps: []
      };
    } else {
      convertedParams = {
        ...algorithmParams,
        source: algorithmParams.source.split(','),
        'vertex-type': algorithmParams.vertexType,
        'vertex-property': fromPairs(
          algorithmParams.vertexProperty.map(
            ([key, value]: [string, string]) => [key, value.split(',')]
          )
        )
      };

      delete convertedParams.vertexType;
      delete convertedParams.vertexProperty;
    }

    (algorithmParams as CustomPathParams).steps.forEach((step, stepIndex) => {
      const convertedStep: Record<string, any> = {};

      delete step.uuid;

      if (step.default_weight !== '') {
        step.weight_by = step.default_weight;
      }

      delete step.default_weight;

      Object.entries(step).forEach(([key, value]) => {
        convertedStep[
          trimEnd(
            translator(
              `data-analyze.algorithm-forms.custom-path.options.${key}`
            ),
            ':'
          )
        ] = value;
      });

      convertedParams.steps[stepIndex] = convertedStep;
    });

    algorithmParams = convertedParams;
  }

  Object.entries(algorithmParams).forEach(([key, value]) => {
    value = isObject(value) ? JSON.stringify(value, null, 4) : value;

    // label value could be null when it means all
    if (key === 'label' && (value === null || value === '__all__')) {
      value = translator(
        `data-analyze.algorithm-forms.${AlgorithmInternalNameMapping[algorithmType]}.pre-value`
      );
    }

    // personal rank customized
    if (algorithmType === 'personalrank' && key === 'with_label') {
      value = translator(
        `data-analyze.algorithm-forms.personal-rank.with-label-radio-value.${(value as string).toLowerCase()}`
      );
    }

    const fullPathKey = `data-analyze.algorithm-forms.${AlgorithmInternalNameMapping[algorithmType]}.options.${key}`;

    // remove redundant key from server
    if (!i18n.exists(fullPathKey)) {
      return;
    }

    key = translator(
      `data-analyze.algorithm-forms.${AlgorithmInternalNameMapping[algorithmType]}.options.${key}`
    );

    statements.push(`${key} ${value}`);
  });

  return statements;
}
