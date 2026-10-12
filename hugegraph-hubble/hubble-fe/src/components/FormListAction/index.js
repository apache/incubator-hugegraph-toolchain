/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {Button} from 'antd';
import {useCallback} from 'react';

const FormListRemove = ({remove, index, afterRemove, children}) => {
    const handleClick = useCallback(() => {
        remove(index);
        afterRemove?.();
    }, [afterRemove, index, remove]);

    return <Button type='link' onClick={handleClick}>{children}</Button>;
};

const FormListAdd = ({add, children}) => {
    const handleClick = useCallback(() => add(), [add]);

    return (
        <Button type='link' className='form_attr_add' onClick={handleClick}>
            {children}
        </Button>
    );
};

export {FormListAdd, FormListRemove};
