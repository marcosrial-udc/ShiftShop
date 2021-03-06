import {combineReducers} from 'redux';

import * as actionTypes from './actionTypes';

const initialState = {
    roles: [],
    user: null,
    usersSearch: null,
    blockedUsersSearch: null
};

const roles = (state = initialState.roles, action) => {

    switch (action.type) {

        case actionTypes.GET_ROLES_COMPLETED:
            return action.roles;

        default:
            return state;
    }

};

const user = (state = initialState.user, action) => {

    switch (action.type) {

        case actionTypes.LOGIN_COMPLETED:
            return action.authenticatedUser.user;

        case actionTypes.LOGOUT:
            return initialState.user;

        default:
            return state;
    }

};

const usersSearch = (state = initialState.usersSearch, action) => {

    switch (action.type) {

        case actionTypes.GET_USERS_COMPLETED:
            return action.usersSearch;

        case actionTypes.CLEAR_USERS_SEARCH:
            return initialState.usersSearch;

        default:
            return state;
    }

};

const reducer = combineReducers({
    roles,
    user,
    usersSearch
});

export default reducer;
