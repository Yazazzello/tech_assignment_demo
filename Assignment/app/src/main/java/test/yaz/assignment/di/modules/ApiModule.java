package test.yaz.assignment.di.modules;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import test.yaz.assignment.restapi.FoursquareApi;


@Module(includes = {NetworkModule.class})
public class ApiModule {
	@Provides
	@Singleton
	public FoursquareApi provideFoursquareApi(Retrofit retrofit) {
		return retrofit.create(FoursquareApi.class);
	}
}
