const key = 'user_'; // 防止缓存问题

const setUser = user => {
    sessionStorage.setItem(key, JSON.stringify(user));
};

const getUser = () => {
    let userStr = sessionStorage.getItem(key);
    if (userStr) {
        return JSON.parse(userStr);
    }

    return null;
};

const clearUser = () => {
    sessionStorage.removeItem(key);
};

const getDefaultGraphspace = () => {
    const user = getUser();

    if (!user) {
        return '';
    }

    if (user.is_superadmin) {
        return 'DEFAULT';
    }

    if (user.resSpaces && user.resSpaces.length > 0) {
        return user.resSpaces[0];
    }

    return '';
};

const isAdmin = () => {
    const user = getUser();

    return user.is_superadmin;
};

export {setUser, getUser, clearUser, getDefaultGraphspace, isAdmin};
