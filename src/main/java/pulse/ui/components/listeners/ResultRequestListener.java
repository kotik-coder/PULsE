package pulse.ui.components.listeners;

public interface ResultRequestListener {

    public void onMergeRequest();

    public void onDeleteRequest();

    public void onPreviewRequest();

    public void onUndoRequest();

    public void onExportRequest();

}
