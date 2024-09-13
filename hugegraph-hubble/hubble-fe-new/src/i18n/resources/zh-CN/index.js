import {merge} from 'lodash-es';
import {
    Bar,
    Common,
} from './components';
const translation = {
    translation: merge(
        Bar,
        Common
    ),
};

export default translation;
