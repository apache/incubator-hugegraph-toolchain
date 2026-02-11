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

import request from './request';
import qs from 'qs';

// login
const login = data => {
    return request.post('/auth/login', data);
};

const logout = () => {
    return request.get('/auth/logout');
};

const status = () => {
    return request.get('/auth/status');
};

const getUserList = params => {
    return request.get('/auth/users/list', {params});
};

const getAllUserList = params => {
    return request.get('/auth/users', {params});
};

const getUserInfo = username => {
    return request.get(`/auth/users/${username}`);
};

const updateUser = (id, data) => {
    return request.put(`/auth/users/${id}`, data);
};

const delUser = id => {
    return request.delete(`/auth/users/${id}`);
};

const updateAdminspace = (username, data) => {
    return request.post(`/auth/users/updateadminspace/${username}`, data);
};

const addUser = data => {
    return request.post('/auth/users', data);
};

const updatePwd = (username, oldpwd, newpwd) => {
    return request.post('/auth/users/updatepwd', {username, oldpwd, newpwd});
};

const importUserUrl = '/api/v1.3/auth/users/batch';

export {login, logout, status, getUserList, getAllUserList, getUserInfo, delUser,
    updateUser, addUser, updatePwd, importUserUrl, updateAdminspace};

// resource

const getResourceList = (graphspace, params) => {
    return request.get(`/graphspaces/${graphspace}/auth/targets`, {params});
};

const addResource = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/auth/targets`, data);
};

const updateResource = (graphspace, id, data) => {
    return request.put(`/graphspaces/${graphspace}/auth/targets/${id}`, data);
};

const getResource = (graphspace, id) => {
    return request.get(`/graphspaces/${graphspace}/auth/targets/${id}`);
};

const delResource = (graphspace, id) => {
    return request.delete(`/graphspaces/${graphspace}/auth/targets/${id}`);
};

export {getResourceList, addResource, updateResource, getResource, delResource};

// role

const getRoleList = (graphspace, params) => {
    return request.get(`/graphspaces/${graphspace}/auth/roles`, {params});
};

const getAllRoleList = graphspace => {
    return request.get(`/graphspaces/${graphspace}/auth/roles/list`);
};

const addRole = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/auth/roles`, data);
};

const updateRole = (graphspace, id, data) => {
    return request.put(`/graphspaces/${graphspace}/auth/roles/${id}`, data);
};

const delRole = (graphspace, id) => {
    return request.delete(`/graphspaces/${graphspace}/auth/roles/${id}`);
};

const delRoleBatch = (graphspace, data) => {
    return request.delete(`/graphspaces/${graphspace}/auth/roles/`, data);
};

const getRoleResourceList = (graphspace, params) => {
    return request.get(`/graphspaces/${graphspace}/auth/accesses`, {params});
};

const addRoleResource = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/auth/accesses`, data);
};

const updateRoleResource = (graphspace, data) => {
    return request.put(`/graphspaces/${graphspace}/auth/accesses`, data);
};

const delRoleResource = (graphspace, data) => {
    return request.delete(`/graphspaces/${graphspace}/auth/accesses`, data);
};

const getRoleUser = (graphspace, params) => {
    return request.get(`/graphspaces/${graphspace}/auth/belongs`, {params});
};

const addRoleUser = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/auth/belongs`, data);
};

const addRoleUserBatch = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/auth/belongs/ids`, data);
};

const delRoleUser = (graphspace, id) => {
    return request.delete(`/graphspaces/${graphspace}/auth/belongs/${id}`);
};

const delRoleUserBatch = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/auth/belongs/delids`, data);
};

export {
    getRoleList, getAllRoleList, addRole, updateRole, delRole, delRoleBatch,
    getRoleResourceList, addRoleResource, updateRoleResource, delRoleResource,
    getRoleUser, addRoleUser, addRoleUserBatch, delRoleUser, delRoleUserBatch,
};

const getPersonal = () => {
    return request.get('/auth/users/getpersonal');
};

const updatePersonal = params => {
    return request.get('/auth/users/updatepersonal', {params});
};

export {getPersonal, updatePersonal};

const getDashboard = () => {
    return request.get('/dashboard');
};

const getVermeer = () => {
    return request.get('/vermeer');
};

export {getDashboard, getVermeer};

// saas TODO REMOVED
const getUUapList = params => {
    return request.get('/uic/list', {params});
};

const getSuperUser = params => {
    return request.get('/auth/users/super', {params});
};

const addSuperUser = data => {
    return request.post('/auth/users/super',
        qs.stringify(data),
        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
    );
};

const addUuapUser = data => {
    return request.post('/auth/users/uuap',
        qs.stringify(data),
        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
    );

    // return request.post('/auth/users/uuap', data);
};

const removeSuperUser = username => {
    return request.delete(`/auth/users/super/${username}`);
};

const getAccountsList = username => {
    return request.get('/uic/accounts', {params: {username}});
};

export {getUUapList, getSuperUser, addSuperUser, addUuapUser, removeSuperUser, getAccountsList};
