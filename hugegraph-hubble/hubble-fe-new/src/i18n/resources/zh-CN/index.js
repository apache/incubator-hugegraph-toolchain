import {merge} from 'lodash-es';
import {
    Board,
    Common,
    ERView,
} from './components';
import {
    Home,
    Manage,
    Analysis,
} from './modules';

const translation = {
    translation: merge(
        Board,
        Common,
        Home,
        Manage,
        ERView,
        Analysis
    ),
};

export default translation;
