/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {render, screen} from '@testing-library/react';
import AdminItem from './index';

jest.mock('../Item', () => ({btnTitle, listData}) => (
    <section>
        <h2>{btnTitle}</h2>
        {listData.map(item => <a key={item.url} href={item.url}>{item.title}</a>)}
    </section>
));

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => ({
        'navigation_page.step3': 'System Management',
        'navigation_page.account_manage': 'Account Management',
    })[key] || key}),
}));

test('only exposes implemented system management destinations', () => {
    render(<AdminItem />);

    expect(screen.getByRole('heading')).toHaveTextContent('System Management');
    expect(screen.getByRole('link', {name: 'Account Management'}))
        .toHaveAttribute('href', '/account');
    expect(screen.queryByText(/Super|Resource|Role/)).toBeNull();
});
