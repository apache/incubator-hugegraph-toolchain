/**
 * @file TaskNavigateView
 * @author
 */

import React, {useCallback} from 'react';
import FinishedIcon from '../../../assets/ic_done_144.svg';
import {useNavigate} from 'react-router-dom';
import c from './index.module.scss';

const TaskNavigateView = props => {
    const {
        message,
        taskId,
    } = props;


    const navigate = useNavigate();

    const onClickDetail = useCallback(
        () => {
            navigate(`/asyncTasks/${taskId}`);
        },
        [navigate, taskId]
    );

    return (
        <div className={c.graphView}>
            <img
                src={FinishedIcon}
                alt="提交成功"
            />
            <span>{message}</span>
            <span>任务ID: {taskId}</span>
            <span>
                <a onClick={onClickDetail}>
                    查看
                </a>
            </span>
        </div>
    );
};

export default TaskNavigateView;