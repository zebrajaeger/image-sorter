package de.zebrajaeger.autopanaopathrelativizer;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class Ui extends JFrame implements DropTargetListener {

    public static void main(String[] args) {
        Ui app = new Ui();
        app.setVisible(true);
    }

    public Ui() {
        setTitle("Pano File or folder drop");
        setSize(200, 200);
        setPreferredSize(new Dimension(400, 400));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setAlwaysOnTop(true);

        new DropTarget(this, DnDConstants.ACTION_COPY, this);
    }

    @Override
    public void dragEnter(DropTargetDragEvent e) {
        checkAcceptance(e);
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public void dragOver(DropTargetDragEvent e) {
    }

    @Override
    public void drop(DropTargetDropEvent e) {
        e.acceptDrop(DnDConstants.ACTION_COPY);
        List<File> files = getFilesFromEvent(e);
        if (isFileSupported(files)) {
//            new Relativizer(FileNameTransformer.DEFAULT).relativizeAllFiles(files);
            // TODO
        } else {
            e.dropComplete(false);
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent ignoreDtde) {
    }

    private void checkAcceptance(DropTargetDragEvent e) {
        if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> files = getFilesFromEvent(e);
            if (isFileSupported(files)) {
                e.acceptDrag(DnDConstants.ACTION_COPY);
                return;
            }
        }
        e.rejectDrag();
    }

    private boolean isFileSupported(List<File> files) {
        for (File f : files) {
            if (!isFileSupported(f)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFileSupported(File file) {
        if (file.isDirectory()) {
            return true;
        }
        return file.getName().toLowerCase().endsWith(".pano");
    }

    private List<File> getFilesFromEvent(DropTargetEvent e) {
        List<File> files = new ArrayList<>();
        try {
            Transferable transferable = null;
            if (DropTargetDragEvent.class.equals(e.getClass())) {
                transferable = ((DropTargetDragEvent) e).getTransferable();
            }
            if (DropTargetDropEvent.class.equals(e.getClass())) {
                transferable = ((DropTargetDropEvent) e).getTransferable();
            }

            if (transferable != null && transferable.isDataFlavorSupported(
                    DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> fileList = (List<File>) transferable.getTransferData(
                        DataFlavor.javaFileListFlavor);
                for (File file : fileList) {
                    if (file.isFile()) {
                        files.add(file);
                    }
                }
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
        }
        return files;
    }
}
