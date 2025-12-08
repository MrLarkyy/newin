package lol.farsight.newin.api.inject;

public final class ReturnRedirect {
    private boolean redirected = false;

    public void redirect() {
        redirected = true;
    }

    public boolean redirected() {
        return redirected;
    }
}
