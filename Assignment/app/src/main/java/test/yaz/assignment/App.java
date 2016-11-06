package test.yaz.assignment;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import rx.plugins.RxJavaHooks;
import test.yaz.assignment.di.AppComponent;
import test.yaz.assignment.di.DaggerAppComponent;
import test.yaz.assignment.di.modules.ContextModule;
import timber.log.Timber;

public class App extends Application {

    private AppComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Stetho.initializeWithDefaults(this);
            RxJavaHooks.setOnError(throwable -> Timber.e(throwable, "error"));
        }

        applicationComponent = DaggerAppComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
    }

    public AppComponent getAppComponent() {
        return applicationComponent;
    }

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }
}
