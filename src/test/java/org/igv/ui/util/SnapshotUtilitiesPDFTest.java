package org.igv.ui.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.igv.ui.panel.Paintable;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static org.junit.Assert.*;

/**
 * Verifies the vector PDF snapshot export: file type recognition,
 * single-page export validity, and vertical tiling of tall snapshots
 * beyond the PDF 14400pt page limit.
 */
public class SnapshotUtilitiesPDFTest {

    /** Minimal Paintable drawing vector primitives plus text, like a track would. */
    static class StubPaintable extends JPanel implements Paintable {
        private final int snapshotHeight;

        StubPaintable(int width, int snapshotHeight) {
            this.snapshotHeight = snapshotHeight;
            setSize(width, snapshotHeight);
        }

        @Override
        public void paintOffscreen(Graphics2D g, Rectangle rect, boolean batch) {
            g.setColor(Color.BLUE);
            g.fillRect(10, 10, getWidth() - 20, 50);
            g.setColor(Color.BLACK);
            g.drawString("igv pdf export test", 20, 40);
            g.setColor(Color.RED);
            g.fillRect(10, snapshotHeight - 60, 100, 50);
        }

        @Override
        public int getSnapshotHeight(boolean batch) {
            return snapshotHeight;
        }
    }

    @Test
    public void testPdfFileTypeRecognized() {
        assertEquals(ImageFileTypes.Type.PDF, ImageFileTypes.getImageFileType(".pdf"));
    }

    @Test
    public void testExportPdfSinglePage() throws Exception {
        StubPaintable paintable = new StubPaintable(800, 600);
        File out = File.createTempFile("igv_snapshot_test_", ".pdf");
        out.deleteOnExit();
        try {
            String result = SnapshotUtilities.doComponentSnapshot(paintable, out, ImageFileTypes.Type.PDF, true);
            assertEquals("OK", result);
            assertTrue(out.length() > 0);
            try (PDDocument doc = Loader.loadPDF(out)) {
                assertEquals(1, doc.getPages().getCount());
                PDPage page = doc.getPages().get(0);
                assertEquals(800f, page.getMediaBox().getWidth(), 0.01f);
                assertEquals(600f, page.getMediaBox().getHeight(), 0.01f);
            }
        } finally {
            out.delete();
        }
    }

    @Test
    public void testExportPdfTilesTallSnapshot() throws Exception {
        StubPaintable paintable = new StubPaintable(800, 15000);
        File out = File.createTempFile("igv_snapshot_tall_test_", ".pdf");
        out.deleteOnExit();
        try {
            String result = SnapshotUtilities.doComponentSnapshot(paintable, out, ImageFileTypes.Type.PDF, true);
            assertEquals("OK", result);
            try (PDDocument doc = Loader.loadPDF(out)) {
                assertEquals(2, doc.getPages().getCount());
            }
        } finally {
            out.delete();
        }
    }
}
