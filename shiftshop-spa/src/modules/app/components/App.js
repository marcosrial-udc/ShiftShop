import React, {Fragment, useEffect} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {useSnackbar} from 'notistack';

import backend from '../../../backend';

import users, {LoginPortal} from '../../users';
import Main from './Main';
import NetworkErrorMessage from './NetworkErrorMessage';

const reauthenticationCallback = dispatch => () => dispatch(users.actions.logout());

const App = () => {
    const dispatch = useDispatch();
    const loggedIn = useSelector(users.selectors.isLoggedIn);
    const { enqueueSnackbar } = useSnackbar();

    useEffect(() => {
        const netError = <NetworkErrorMessage/>;

        /* Configure backend proxy. */
        backend.init(_ => enqueueSnackbar(netError,
            {
                variant: 'error',
                anchorOrigin: {vertical: 'top', horizontal: 'center'},
                preventDuplicate: true,
                autoHideDuration: 2000,
            }));

        dispatch(
            users.actions.tryLoginFromServiceToken(
                () => true,
                reauthenticationCallback(dispatch)));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <Fragment>
            {loggedIn && <Main/>}
            {!loggedIn && <LoginPortal/>}
        </Fragment>
    );
};

export default App;
