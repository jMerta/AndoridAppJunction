package com.tmejs.andoridappjunction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.tmejs.andoridappjunction.activities.JoinGameActivity;
import com.tmejs.andoridappjunction.activities.StartingGameActivity;
import com.tmejs.andoridappjunction.activities.system.MyActivity;
import com.tmejs.andoridappjunction.activities.system.WaitingActivity;
import com.tmejs.andoridappjunction.domain.AdminSignedIn;
import com.tmejs.andoridappjunction.domain.AskDoGameExist;
import com.tmejs.andoridappjunction.domain.AskDoGameExistResponse;
import com.tmejs.andoridappjunction.domain.Competition;
import com.tmejs.andoridappjunction.domain.GameResult;
import com.tmejs.andoridappjunction.domain.Player;
import com.tmejs.andoridappjunction.domain.StartGame;
import com.tmejs.andoridappjunction.usables.MyAsyncTask;
import com.tmejs.andoridappjunction.utils.TCPUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tmejs on 25.11.2017.
 */

public class ApplicationController {

    //region Statyczne zmienne ważne w aplikacji
    public static AppParams APP_PARAMS;
    private static ApplicationController appController;
    public static AsyncHelper ASYNC_HELPER;
    public static ViewsController VIEWS_CONTROLLER = ViewsController.getInstance();


    public ApplicationController getInstance() {
        if (appController == null) {
            appController = new ApplicationController();
        }
        return appController;
    }

    //region Eventy aplikacji
    //Event dający możliwość reagowania na stan aplikacji
    private static OnRollingEvent onRollingEvent;
    private static OnMinimalizeEvent onMinimalizeEvent;
    private static OnMaxymalizeEvent onMaxymalizeEvent;
    private static OnDestroyEvent onDestroyEvent;


    private static AfterActivityChanged afterActivityChanged;

    public static void closeApp() {
        System.exit(-1);
    }


    public static String getRNameByID(Integer viewId) {
        if (viewId == 0xffffffff) return "";
        return getCurrentActivity().getResources().getResourceEntryName(viewId);
    }

    public static void startNewGame(final StartGame startGameObject) {
        switchAppToWaitingMode();

        ApplicationController.APP_PARAMS.setParamValue(AppParams.INIITIAL_PLAYER_AMOUNT,startGameObject.player.initialBillAmount);
        ApplicationController.APP_PARAMS.setParamValue(AppParams.PLAYER_NAME,startGameObject.player.name);
        ApplicationController.APP_PARAMS.setParamValue(AppParams.PLAYER_ID,0);


        ApplicationController.ASYNC_HELPER.executeAsync(new MyAsyncTask(new MyAsyncTask.RequestEvent() {
            @Override
            public Object request() {
                Gson gson = new Gson();
                String jsonRepresentation = gson.toJson(startGameObject);
                Log.e("jsoning", jsonRepresentation);
                try {
                    return TCPUtil.sendRequest(jsonRepresentation);
                } catch (IOException e) {
                    Log.e("PostRequest starting gagme:","",e);
                    return "";
                }
            }
            @Override
            public void postRequest(Object params) {
                 Log.e("PostRequest starting gagme:",(String)params);
                GameWrapper.analyzeStartGameResponse(params);

            }
        }));

    }

    public static void waitForAllPlayersToSignIn() {
    switchAppToWaitingMode();

        ApplicationController.ASYNC_HELPER.executeAsync(new MyAsyncTask(new MyAsyncTask.RequestEvent() {
            @Override
            public Object request() {
                Gson gson = new Gson();
                Player player = new Player();
                player.name= ApplicationController.APP_PARAMS.getParamValue(AppParams.PLAYER_NAME);

                try {
                    player.initialBillAmount=new Long(ApplicationController.APP_PARAMS.getParamValue(AppParams.INIITIAL_PLAYER_AMOUNT));
                } catch (NumberFormatException e) {
                    Log.e("waitForAllPlayersToSignIn()","Jakis blasd",e);
                }

                String jsonString = gson.toJson(player);

                try {
                    return TCPUtil.sendRequest(AppParams.HTTP_PROTOCOL_PREFIX+AppParams.WEB_SERWER_ADDRESS+ApplicationController.APP_PARAMS.getParamValue(AppParams.COMPETITION_ID)+AppParams.JOIN_SERVICE_NAME,jsonString);
                } catch (IOException e) {
                    Log.e("waitForAllPlayersToSignIn()","Jakis blasd",e);
                    return "";
                }
            }

            @Override
            public void postRequest(Object params) {
                GameWrapper.analyzePlayersStatusResponse(params);
            }
        }));

    }

    public static void startNewRound() {
        switchAppToWaitingMode();

        ApplicationController.ASYNC_HELPER.executeAsync(new MyAsyncTask(new MyAsyncTask.RequestEvent() {
            @Override
            public Object request() {

                Gson gson = new Gson();
                Player player = new Player();
                Log.e("PLayerID",ApplicationController.APP_PARAMS.getParamValue(AppParams.PLAYER_ID));
                player.id = Long.valueOf(ApplicationController.APP_PARAMS.getParamValue(AppParams.PLAYER_ID));
                Log.e("PLayerID",player.id.toString());
                String jsonRepresentation = gson.toJson(player);
                Log.e("jsoning", jsonRepresentation);
                try {
                    return TCPUtil.sendRequest( AppParams.HTTP_PROTOCOL_PREFIX + AppParams.WEB_SERWER_ADDRESS +ApplicationController.APP_PARAMS.getParamValue(AppParams.COMPETITION_ID) +AppParams.GET_NEW_GAME_SERVLET_PATH ,jsonRepresentation);
                } catch (IOException e) {
                    Log.e("startNewRound()","Jakis blasd",e);
                    return "";
                }
            }

            @Override
            public void postRequest(Object params) {
                Log.e("postRequest()",(String)params);
                GameWrapper.analyzeStartNewRoundResponse(params);
            }
        }));
    }

    public static void sendGameResult(final String result, final long resultTime) {
        switchAppToWaitingMode();

        ApplicationController.ASYNC_HELPER.executeAsync(new MyAsyncTask(new MyAsyncTask.RequestEvent() {
            @Override
            public Object request() {

                Gson gson = new Gson();
                GameResult gaeResult = new GameResult();
                gaeResult.response=result;
                gaeResult.time=resultTime;
                gaeResult.userId = new Integer(ApplicationController.APP_PARAMS.getParamValue(AppParams.PLAYER_ID));
                String jsonRepresentation = gson.toJson(gaeResult);
                try {
                    return TCPUtil.sendRequest(AppParams.HTTP_PROTOCOL_PREFIX + AppParams.WEB_SERWER_ADDRESS +ApplicationController.APP_PARAMS.getParamValue(AppParams.COMPETITION_ID) +AppParams.GET_RESULT_SERLET_PATH,jsonRepresentation);
                } catch (IOException e) {
                    Log.e("sendGameResult()","Jakis blad",e);
                    return "";
                }
            }

            @Override
            public void postRequest(Object params) {
                GameWrapper.analyzePlayersStatusResponse(params);
            }
        }));
    }

    public static void askDoGameExist(final String text) {
        ApplicationController.ASYNC_HELPER.executeAsync(new MyAsyncTask(new MyAsyncTask.RequestEvent() {
            @Override
            public Object request() {
                Gson gson = new Gson();
                AskDoGameExist doGameExist = new AskDoGameExist();
                doGameExist.gameId = new Integer(text);

                String jsonRepresentation = gson.toJson(doGameExist);
                try {
                    return TCPUtil.sendRequest(AppParams.HTTP_PROTOCOL_PREFIX + AppParams.WEB_SERWER_ADDRESS + AppParams.WEB_ASK_GAME_SERVLET_ADDRESS , jsonRepresentation);
                } catch (IOException e) {
                    Log.e("askDoGameExist","tex="+text,e);
                    return "";
                }
            }

            @Override
            public void postRequest(Object params) {
                Activity curAct= ApplicationController.getCurrentActivity();
                Gson gson = new Gson();
                AskDoGameExistResponse resp = gson.fromJson((String)params,AskDoGameExistResponse.class);

                //Sprawdzenie czy cały czas jesteśmy w danym activity
                try {
                    ((JoinGameActivity)curAct).setValidationResult(resp);
                } catch (Exception e) {
                    Log.e("postRequest","params="+(String)params,e);
                    showNews(getStringFromResources(R.string.TXT_WRONG_GAME_NUMBER));
                }

            }
        }));
    }

    public interface AfterActivityChanged {
        void afterActivityChanged();
    }

    public static void setStoped(MyActivity activity) {
        //jeśli aktualne Activity paused to zminimalizowany ekran
        //
        Log.i("ApplicationControler", "setStoped");
        if (isCurrentPaused && !isActivityChanging) {
            setIsMinimalized(true);
            onMinimalizeEvent();
        } else {
            setLastActivity(activity);
        }
    }

    public Boolean isActivitychanging() {
        return isActivityChanging;
    }

    public interface OnRollingEvent {
        /**
         * Interakcja na obrót ekranu
         */
        void onRollingEvent();

    }

    public static void runAsyncRequest(MyAsyncTask request) {
        ASYNC_HELPER.executeAsync(request);
    }


    public static String getStringFromResources(Integer stringId) {
        return getCurrentActivity().getResources().getString(stringId);
    }


    public interface OnMinimalizeEvent {
        /**
         * Interakcja na minimalizację aplikacji
         */
        void onMinimalizeEvent();
    }


    public interface OnMaxymalizeEvent {
        /**
         * Interakcja na minimalizację aplikacji
         */
        void onMaxymalizeEvent();
    }

    public interface OnDestroyEvent {

        /**
         * Interacja przy zamykaniu aplikacji
         */
        void onDestroyEvent();
    }


    //endregion

    //Context aplikacji
    //Ostatnie activity
    private static MyActivity lastActivity;
    //Aktualne activity
    private static MyActivity currentActivity;

    //Czy aktualny thread zatrzymany
    private static Boolean isCurrentPaused = false;

    //Informacja czy jesteśmy w trakcie zmieniania activity
    private static Boolean isActivityChanging = false;

    //Informacja czy aplikacja została zminimalizowanaf
    private static Boolean isMinimalized = false;
    //endregion


    //region Gettery i Settery klasy. Najlepiej nie dotykać bo się może posypać.

    /**
     * Pobranie aktualnie wyświetlanego activity
     *
     * @return
     */
    public static MyActivity getCurrentActivity() {
        return currentActivity;
    }


    public static MyActivity getLastActivity() {
        return lastActivity;
    }


    public static Boolean isMinimalized() {
        return isMinimalized;
    }

    public static void setCurrentActivity(MyActivity activity) {
        lastActivity = currentActivity;
        currentActivity = activity;
    }

    public static void setLastActivity(MyActivity la) {
        lastActivity = la;
    }

    public static void setIsCurrentPaused(Boolean isPaused) {
        Log.i("ApplicationControler", "setIsCurrentPaused -> " + isPaused);
        //UStawiamy że activity jest spauzowane
        isCurrentPaused = isPaused;
    }

    public static void setActivityChanging(Boolean isChanging) {
        Log.i("ApplicationControler", "setActivityChanging");
        isActivityChanging = isChanging;
    }

    public static void onRollingEvent() {
        Log.i("ApplicationControler", "onRollingEvent");
        isCurrentPaused = false;

        //Wywołanie eventu onRolling
        if (onRollingEvent != null) {
            onRollingEvent.onRollingEvent();
        }
    }

    public static void runOnUiThread(Runnable runnable) {
        ASYNC_HELPER.executeOnUi(runnable);
    }

    public static void setIsMinimalized(Boolean isMin) {
        Log.v("ApplicationControler", "setIsMinimalized  " + isMin);
        isMinimalized = isMin;
    }


    public static void ActivityResumed(MyActivity activity) {
        Log.v("ApplicationControler", "ActivityResumed  ");
        setIsMinimalized(false);
        setIsCurrentPaused(false);
        setCurrentActivity(activity);


        if (isActivityChanging) {
            //wywołanie eventu po zmianie activity
            if (afterActivityChanged != null) {
                afterActivityChanged.afterActivityChanged();
                afterActivityChanged = null;
            }
        }

        setActivityChanging(false);

    }

    public static void setIsRestarting() {
        Log.v("ApplicationControler", "setIsRestarting  ");
        if (isMinimalized) {
            onMaxymalizeEvent();
            setIsMinimalized(false);
        }
    }

    public static void onDestroyEvent() {
        Log.v("ApplicationControler", "onDestroyEvent");
        if (isMinimalized) onAppDestroyEvent();
    }


    private static void onAppDestroyEvent() {
        Log.e("ApplicationControler", "onAppDestroyEvent");
        if (onDestroyEvent != null) {
            onDestroyEvent.onDestroyEvent();
        }
    }

    private static void onMinimalizeEvent() {
        Log.e("ApplicationControler", "onMinimalizeEvent");

        //Wywołanie eventu onMinimalize
        if (onMinimalizeEvent != null) {
            onMinimalizeEvent.onMinimalizeEvent();
        }
    }

    private static void onMaxymalizeEvent() {
        Log.e("ApplicationControler", "onMaxymalizeEvent");

        //Wywołanie eventu onMaxymalize
        if (onMaxymalizeEvent != null) {
            onMaxymalizeEvent.onMaxymalizeEvent();
        }
    }

    public static void setOnRollingEvent(OnRollingEvent onRollingEvent) {
        ApplicationController.onRollingEvent = onRollingEvent;
    }


    public static void showNews(final String txt) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getCurrentActivity(), txt, Toast.LENGTH_LONG).show();
            }
        });

    }


    public static void setOnMinimalizeEvent(OnMinimalizeEvent onMinimalizeEvent) {
        ApplicationController.onMinimalizeEvent = onMinimalizeEvent;
    }

    public static void setOnMaxymalizeEvent(OnMaxymalizeEvent onMaxymalizeEvent) {
        ApplicationController.onMaxymalizeEvent = onMaxymalizeEvent;
    }

    public static void setOnDestroyEvent(OnDestroyEvent onDestroyEvent) {
        ApplicationController.onDestroyEvent = onDestroyEvent;
    }

    /**
     * Zmiana activity
     *
     * @param activityClass Klasa docelowego acitivty
     * @return
     */
    public static Boolean switchActivity(Class activityClass) {
        Log.i(ApplicationController.class.toString(), "switchActivity(" + activityClass.toString() + ")");
        Intent newIntent = new Intent(getCurrentActivity(), activityClass);
        getCurrentActivity().startActivity(newIntent);
        return true;
    }


    /**
     * Zmiana activity z evetem wywoływanym po pojawieniu się activity na ekranie
     *
     * @param activityClass
     * @param aac
     * @return
     */
    public static Boolean switchActivity(Class activityClass, AfterActivityChanged aac) {
        Log.i(ApplicationController.class.toString(), "switchActivity(" + activityClass.toString() + ") with event");
        afterActivityChanged = aac;
        Intent newIntent = new Intent(getCurrentActivity(), activityClass);
        getCurrentActivity().startActivity(newIntent);
        return true;
    }


    //endregion


    //region Inicjalizacja klas w aplikacji

    /**
     * Inicjalizacja klas do statycznych zmiennych
     *
     * @param startActivity
     */
    public static void init(MyActivity startActivity) {
        Log.e("AppControl", "init()");
        initAppParams(startActivity);
        Log.e("AppControl", "AppParams Initialized");
        intitAsyncHelper(startActivity);


    }


    public static void intitAsyncHelper(MyActivity activity) {
        ASYNC_HELPER = AsyncHelper.getInstance(activity);
    }



    /**
     * Inicjalizacja parametrów aplikacji na podstawie SharedPreferences podanego Activity
     *
     * @param startActivity Activity którego shared preferences będziemy pobierać.
     */
    private static void initAppParams(MyActivity startActivity) {
        if (ApplicationController.APP_PARAMS == null)
            ApplicationController.APP_PARAMS = AppParams.getInstance(startActivity.getSharedPreferences(AppParams.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE));
    }

    private static void switchAppToWaitingMode() {
        ApplicationController.switchActivity(WaitingActivity.class);
    }


//    public static void startTestGame() {
//        switchAppToWaitingMode();
//
//        final Competition comp = new Competition();
//        comp.admin = new Player();
//        comp.admin.avatarId = Long.getLong("1");
//        comp.admin.name = "Mati";
//        comp.numberOfPlayers = 5;
//
//
//        ApplicationController.ASYNC_HELPER.executeAsync(new MyAsyncTask(new MyAsyncTask.RequestEvent<String>() {
//            @Override
//            public String request() {
//                Gson gson = new Gson();
//                String jsonRepresentation = gson.toJson(comp);
//                Log.e("jsoning", jsonRepresentation);
//                try {
//                    return TCPUtil.sendRequest(jsonRepresentation);
//                } catch (IOException e) {
//                    return "";
//                }
//            }
//
//            @Override
//            public void postRequest(String params) {
//                GameWrapper.anaylzeResponse(params);
//            }
//        }));
//
//
//    }

}
