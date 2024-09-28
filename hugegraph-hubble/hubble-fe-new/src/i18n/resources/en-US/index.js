import {merge} from 'lodash-es';
import {
    Board,
    Common,
    ERView,
} from './components';
import {
    Home,
    Manage,
} from './modules';

const translation = {
    translation: merge(
        Board,
        Common,
        Home,
        Manage,
        ERView
    ),
};

export default translation;
