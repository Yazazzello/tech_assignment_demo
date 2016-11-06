package test.yaz.assignment.di;

import javax.inject.Singleton;

import dagger.Component;
import test.yaz.assignment.MapsActivity;
import test.yaz.assignment.di.modules.ApiModule;
import test.yaz.assignment.di.modules.ContextModule;

@Singleton
@Component(modules = {ContextModule.class, ApiModule.class})
public interface AppComponent {

	void inject(MapsActivity activity);

}
