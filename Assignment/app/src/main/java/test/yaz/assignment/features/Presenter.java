package test.yaz.assignment.features;


import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class Presenter<T extends MvpView> {

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    private T mvpView;

    public void attachView(T mvpView) {
        this.mvpView = mvpView;
    }

    public void detachView() {
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
            compositeSubscription = null;
        }
        this.mvpView = null;
    }

    public boolean isViewAttached() {
        return mvpView != null;
    }

    public T getMvpView() {
        return mvpView;
    }

    public void checkViewAttached() {
        if (!isViewAttached()) throw new MvpViewNotAttachedException();
    }

    protected void addSubscription(Subscription subscription){
        compositeSubscription.add(subscription);
    }

    public static class MvpViewNotAttachedException extends RuntimeException {
        public MvpViewNotAttachedException() {
            super("Please call Presenter.attachView(MvpView) before" +
                    " requesting data to the Presenter");
        }
    }
}
