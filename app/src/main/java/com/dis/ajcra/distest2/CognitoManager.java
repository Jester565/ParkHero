package com.dis.ajcra.distest2;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.VerificationHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CognitoManager {
    private static final String COGNITO_USER_POOL_ID = "us-west-2_PkZb6onNf";
    private static final String COGNITO_IDENTITY_POOL_ID = "us-west-2:76a1b798-741a-4a5e-9b7e-4112e4fd0acb";
    private static final String COGNITO_CLIENT_ID = "4sk070sudo8u3qu4qjrnvv513a";
    private static final String COGNITO_CLIENT_SECRET = "1bfhumogg5j2u297nie4fv1u5mn58bn92iq8r8edfv1vtarloapc";
    private static final Regions COGNITO_REGION = Regions.US_WEST_2;
    private static final String COGNITO_USER_POOL_ARN = "cognito-idp.us-west-2.amazonaws.com/us-west-2_PkZb6onNf";

    private static CognitoManager instance = null;

    public static CognitoManager GetInstance(Context appContext) {
        if (instance == null) {
            instance = new CognitoManager(appContext);
        }
        return instance;
    }

    private CognitoManager(Context appContext) {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                appContext, COGNITO_IDENTITY_POOL_ID, COGNITO_REGION
        );
        ClientConfiguration clientConf = new ClientConfiguration();
        AmazonCognitoIdentityProviderClient identityProviderClient = new AmazonCognitoIdentityProviderClient(credentialsProvider, new ClientConfiguration());
        identityProviderClient.setRegion(Region.getRegion(Regions.US_WEST_2));

        userPool = new CognitoUserPool(appContext, COGNITO_USER_POOL_ID, COGNITO_CLIENT_ID, COGNITO_CLIENT_SECRET, identityProviderClient);
        user = userPool.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return (credentialsProvider.getLogins() != null && credentialsProvider.getLogins().size() > 0);
    }

    public String getUserID() {
        if (user != null) {
            return user.getUserId();
        }
        return null;
    }

    public String getFederatedID() {
        return credentialsProvider.getIdentityId();
    }

    public interface RegisterUserHandler {
        void onSuccess();

        void onVerifyRequired(String deliveryMethod, String deliveryDest);

        void onFailure(Exception ex);
    }
    public void registerUser(String userName, String email, final String pwd, final RegisterUserHandler cb) {
        CognitoUserAttributes userAttributes = new CognitoUserAttributes();
        userAttributes.addAttribute("email", email);
        SignUpHandler signUpHandler = new SignUpHandler() {
            @Override
            public void onSuccess(CognitoUser registeredUser, boolean signUpConfirmationState, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
                user = registeredUser;
                if (!signUpConfirmationState) {
                    cb.onVerifyRequired(
                            cognitoUserCodeDeliveryDetails.getDeliveryMedium(),
                            cognitoUserCodeDeliveryDetails.getDestination());
                } else {
                    cb.onSuccess();
                }
            }

            @Override
            public void onFailure(Exception exception) {
                cb.onFailure(exception);
            }
        };
        userPool.signUpInBackground(userName, pwd, userAttributes, null, signUpHandler);
    }

    public void validateUser(String code, final GenericHandler cb) {
        if (user != null) {
            user.confirmSignUpInBackground(code, false, cb);
        } else
        {
            cb.onFailure(new Exception("ValidateUser: user not set"));
        }
    }



    public interface UserAttributesHandler {
        void onSuccess(Map<String, String> attribMap);
        void onFailure(Exception ex);
    }
    public void getUserAttributes(final UserAttributesHandler cb) {
        GetDetailsHandler getDetailsHandler = new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                Map<String,String> attribMap = cognitoUserDetails.getAttributes().getAttributes();
                Iterator<Map.Entry<String, String>> iterator = attribMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    Log.d("STATUS", "Entry " + entry.getKey() + " : " + entry.getValue());
                }
                cb.onSuccess(attribMap);
            }

            @Override
            public void onFailure(Exception exception) {
                cb.onFailure(exception);
            }
        };
        if (user != null) {
            user.getDetailsInBackground(getDetailsHandler);
        } else {
            cb.onFailure(new Exception("GetUserAttributes: User not set"));
        }
    }

    public static abstract class LoginHandler {
        public abstract void onSuccess();
        public abstract void onMFA(MultiFactorAuthenticationContinuation continuation);
        public void onUnverified(Exception ex) {
            onFailure(ex);
        }
        public void onNoUser(Exception ex) {
            onFailure(ex);
        }
        public void onBadPwd(Exception ex) {
            onFailure(ex);
        }
        public abstract void onFailure(Exception ex);
    }

    public void login(String username, final String pwd, final LoginHandler cb)
    {
        if (user.getUserId() != username) {
            user = userPool.getUser(username);
        }
        login(pwd, cb);
    }

    public void login(final String pwd, final LoginHandler cb)
    {
        AuthenticationHandler handler = new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession) {
                addLogin(COGNITO_USER_POOL_ARN, userSession.getIdToken().getJWTToken());
                cb.onSuccess();
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                AuthenticationDetails details = new AuthenticationDetails(userId, pwd, null);
                authenticationContinuation.setAuthenticationDetails(details);
                authenticationContinuation.continueTask();
            }

            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
                cb.onMFA(continuation);
            }

            @Override
            public void onFailure(Exception exception) {
                if (exception instanceof AmazonServiceException) {
                    String errCode = ((AmazonServiceException)exception).getErrorCode();
                    switch(errCode) {
                        case "UserNotConfirmedException":
                            cb.onUnverified(exception);
                            return;
                        case "UserNotFoundException":
                            cb.onNoUser(exception);
                            return;
                    }
                }
                Log.d("STATE", "Login exception " + exception);
                cb.onFailure(exception);
            }
        };
        if (user != null) {
            user.getSessionInBackground(handler);
        } else {
            cb.onFailure(new Exception("Login: user not set"));
        }
    }

    public void addLogin(String provider, String token) {
        Map<String,String> logins = new HashMap<String,String>();
        logins.put(provider, token);
        for (Map.Entry<String,String> login: credentialsProvider.getLogins().entrySet()) {
            Log.d("STATE", "Login: " + login.getKey());
        }
        credentialsProvider.clear();
        for (Map.Entry<String,String> login: credentialsProvider.getLogins().entrySet()) {
            Log.d("STATE", "Login: " + login.getKey());
        }
        credentialsProvider.setLogins(logins);
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                credentialsProvider.refresh();
                Log.d("STATE", "IdentityID: " + credentialsProvider.getIdentityId());
                Log.d("STATE", "Aws AccessID: " + credentialsProvider.getCredentials().getAWSAccessKeyId());
                Log.d("STATE", "Aws Secret: " + credentialsProvider.getCredentials().getAWSSecretKey());
                Log.d("STATE", "Token: " + credentialsProvider.getCredentials().getSessionToken());

                return null;
            }
        };
        task.execute();
    }

    public interface ResetPwdHandler {
        void onSuccess();
        void onContinuation(ForgotPasswordContinuation continuation);
        void onFailure(Exception ex);
    }

    public void resetPwd(String username, final ResetPwdHandler cb) {
        if (user.getUserId() != username) {
            user = userPool.getUser(username);
        }
        resetPwd(cb);
    }

    public void resetPwd(final ResetPwdHandler cb) {
        user.forgotPasswordInBackground(new ForgotPasswordHandler() {
            @Override
            public void onSuccess() {
                cb.onSuccess();
            }

            @Override
            public void getResetCode(ForgotPasswordContinuation continuation) {
                cb.onContinuation(continuation);
            }

            @Override
            public void onFailure(Exception exception) {
                cb.onFailure(exception);
            }
        });
    }

    interface ResendCodeHandler {
        void onSuccess(String delvMeth, String delvDest);
        void onFailure(Exception ex);
    }
    public void resendCode(final ResendCodeHandler handler) {
        user.resendConfirmationCodeInBackground(new VerificationHandler() {
            @Override
            public void onSuccess(CognitoUserCodeDeliveryDetails verificationCodeDeliveryMedium) {
                handler.onSuccess(verificationCodeDeliveryMedium.getDeliveryMedium(), verificationCodeDeliveryMedium.getDestination());
            }

            @Override
            public void onFailure(Exception exception) {
                handler.onFailure(exception);
            }
        });
    }

    public CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    AWSSessionCredentials getCredentials() {
        return credentialsProvider.getCredentials();
    }

    private CognitoUser user;
    private CognitoUserPool userPool;
    private CognitoCachingCredentialsProvider credentialsProvider;
}
