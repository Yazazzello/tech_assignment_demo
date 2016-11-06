package test.yaz.assignment.features;

public interface LoadingView extends MvpView {

    void flipProgress(boolean displayProgress);

    void displayError(String message);
}
