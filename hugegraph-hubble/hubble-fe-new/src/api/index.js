import request from './request';
import * as manage from './manage';
import * as auth from './auth';
import * as analysis from './analysis';
import * as cloud from './cloud';

const uploadUrl = '/api/v1.3/ingest/files/upload';

export {request, manage, auth, analysis, cloud, uploadUrl};
