import { v4 } from 'uuid';

export enum Algorithm {
  loopDetection = 'loop-detection',
  focusDetection = 'focus-detection',
  shortestPath = 'shortest-path',
  shortestPathAll = 'shortest-path-all',
  allPath = 'all-path',
  modelSimilarity = 'model-similarity',
  neighborRankRecommendation = 'neighbor-rank-recommendation',
  realTimeRecommendation = 'real-time-recommendation',
  kStepNeighbor = 'k-step-neighbor',
  kHop = 'k-hop',
  customPath = 'custom-path',
  customIntersectionDetection = 'custom-intersection-detection',
  radiographicInspection = 'radiographic-inspection',
  commonNeighbor = 'common-neighbor',
  weightedShortestPath = 'weighted-shortest-path',
  singleSourceWeightedPath = 'single-source-weighted-path',
  jaccardSimilarity = 'jaccard-similarity',
  personalRankRecommendation = 'personal-rank-recommendation'
}

export function initializeRequestStatus() {
  return {
    fetchAlgorithmResult: 'standby'
  };
}

export function initializeErrorInfo() {
  return {
    fetchAlgorithmResult: {
      code: NaN,
      message: ''
    }
  };
}

export function createLoopDetectionDefaultParams() {
  return {
    source: '',
    direction: 'BOTH',
    max_depth: '',
    label: '__all__',
    max_degree: '10000',
    source_in_ring: true,
    limit: '10',
    capacity: '10000000'
  };
}

export function createValidateLoopDetectionParamsErrorMessage() {
  return {
    source: '',
    direction: '',
    max_depth: '',
    label: '',
    max_degree: '',
    source_in_ring: '',
    limit: '',
    capacity: ''
  };
}

export function createFocusDetectionDefaultParams() {
  return {
    source: '',
    target: '',
    direction: 'BOTH',
    max_depth: '',
    label: '__all__',
    max_degree: '10000',
    limit: '10',
    capacity: '10000000'
  };
}

export function createValidateFocusDetectionParamsErrorMessage() {
  return {
    source: '',
    target: '',
    direction: '',
    max_depth: '',
    label: '',
    max_degree: '',
    skip_degree: '',
    capacity: '',
    limit: ''
  };
}

export function createShortestPathDefaultParams() {
  return {
    source: '',
    target: '',
    direction: 'BOTH',
    max_depth: '',
    label: '__all__',
    max_degree: '10000',
    skip_degree: '0',
    capacity: '10000000',
    limit: '1000000'
  };
}

export function createValidateShortestPathParamsErrorMessage() {
  return {
    source: '',
    target: '',
    direction: '',
    max_depth: '',
    label: '',
    max_degree: '',
    skip_degree: '',
    capacity: '',
    limit: ''
  };
}

export function createShortestPathAllDefaultParams() {
  return {
    source: '',
    target: '',
    direction: 'BOTH',
    max_depth: '',
    label: '__all__',
    max_degree: '10000',
    skip_degree: '0',
    capacity: '10000000'
  };
}

export function createValidateShortestPathAllParamsErrorMessage() {
  return {
    source: '',
    target: '',
    direction: '',
    max_depth: '',
    label: '',
    max_degree: '',
    skip_degree: '',
    capacity: ''
  };
}

export function createAllPathDefaultParams() {
  return {
    source: '',
    target: '',
    direction: 'BOTH',
    max_depth: '',
    label: '__all__',
    max_degree: '10000',
    skip_degree: '0',
    capacity: '10000000',
    limit: '1000000'
  };
}

export function createValidateAllPathParamsErrorMessage() {
  return {
    source: '',
    target: '',
    direction: '',
    max_depth: '',
    label: '',
    max_degree: '',
    skip_degree: '',
    capacity: '',
    limit: ''
  };
}

export function createModelSimilarityDefaultParams() {
  return {
    method: 'id',
    source: '',
    vertexType: '',
    vertexProperty: [],
    direction: 'BOTH',
    least_neighbor: '',
    similarity: '',
    label: '__all__',
    max_similar: '1',
    least_similar: '1',
    property_filter: '',
    least_property_number: '',
    max_degree: '10000',
    skip_degree: '10000000',
    capacity: '10000000',
    limit: '10',
    return_common_connection: false,
    return_complete_info: false
  };
}

export function createValidateModelSimilarParamsErrorMessage() {
  return {
    method: '',
    source: '',
    vertexType: '',
    vertexProperty: '',
    direction: '',
    least_neighbor: '',
    similarity: '',
    label: '',
    max_similar: '',
    least_similar: '',
    property_filter: '',
    least_property_number: '',
    max_degree: '',
    skip_degree: '',
    capacity: '',
    limit: '',
    return_common_connection: '',
    return_complete_info: ''
  };
}

export function createNeighborRankDefaultParams(): {
  source: string;
  alpha: string;
  direction: string;
  capacity: string;
  steps: {
    uuid: string;
    direction: string;
    label: string;
    degree: string;
    top: string;
  }[];
} {
  return {
    source: '',
    alpha: '',
    direction: 'BOTH',
    capacity: '10000000',
    steps: [
      {
        uuid: v4(),
        direction: 'BOTH',
        label: '__all__',
        degree: '10000',
        top: '100'
      }
    ]
  };
}

export function createValidateNeighborRankErrorMessage(): {
  source: string;
  alpha: string;
  direction: string;
  capacity: string;
  steps: {
    uuid: string;
    direction: string;
    label: string;
    degree: string;
    top: string;
  }[];
} {
  return {
    source: '',
    alpha: '',
    direction: '',
    capacity: '',
    steps: [
      {
        uuid: '',
        direction: '',
        label: '',
        degree: '',
        top: ''
      }
    ]
  };
}
