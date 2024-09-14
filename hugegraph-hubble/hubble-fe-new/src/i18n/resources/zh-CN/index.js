import {merge} from 'lodash-es';
import {
    Board,
    Common,
    ERView,
} from './components';
import {
    Algorithm,
    Home,
    Manage,
} from './modules';

const translation = {
    translation: merge(
        Board,
        Common,
        Algorithm,
        Home,
        Manage,
        ERView
    ),
};

export default translation;
