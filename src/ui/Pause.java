package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Pause overlay component. Add this on top of the game panel and toggle visibility
 * when the game is paused/unpaused. The component paints a translucent backdrop
 * and a small generated panel with a title and hint text.
 */
public class Pause extends JComponent {
	private BufferedImage img;

	/** Listener for pause actions (resume/setting/quit) */
	public interface PauseListener {
		void onResume();
		void onSetting();
		void onQuit();
	}

	private int hitTest(int mx, int my) {
		if (iconRegionsScaled == null) return -1;
		for (int i = 0; i < iconRegionsScaled.length; i++) {
			Rectangle r = iconRegionsScaled[i];
			if (r != null && r.contains(mx, my)) return i;
		}
		return -1;
	}

	private PauseListener listener;
	// detected icon regions in image coordinates (image pixel space)
	private Rectangle[] iconRegions = new Rectangle[3];
	// scaled icon regions in component coordinates (after layout)
	private Rectangle[] iconRegionsScaled = new Rectangle[3];
	private int hoverIndex = -1;
	private BufferedImage imgSelect = null;
	private BufferedImage[] iconSelectImgs = new BufferedImage[3];

	public Pause() {
		setOpaque(false);
		// try to load a custom pause image from resources; fall back to generated art
		try {
			java.io.InputStream is = getClass().getResourceAsStream("/ui/Pause.png");
			if (is != null) {
				BufferedImage loaded = ImageIO.read(is);
				// scale to a reasonable size while preserving aspect
				int maxW = 420, maxH = 200;
				int iw = loaded.getWidth();
				int ih = loaded.getHeight();
				double scale = Math.min((double)maxW / iw, (double)maxH / ih);
				if (scale < 1.0) {
					int nw = (int)(iw * scale);
					int nh = (int)(ih * scale);
					BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = scaled.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.drawImage(loaded, 0, 0, nw, nh, null);
					g.dispose();
					img = scaled;
				} else {
					img = loaded;
				}
			} else {
				generateImage();
			}
			// attempt to load optional "select" image (same layout but with hover states)
				try {
					java.io.InputStream is2 = getClass().getResourceAsStream("/ui/PauseSelect.png");
					if (is2 == null) is2 = getClass().getResourceAsStream("/ui/pauseselect.png");
					if (is2 == null) is2 = getClass().getResourceAsStream("/ui/pause_select.png");
					if (is2 != null) {
						imgSelect = ImageIO.read(is2);
					}
					// try per-icon select images: Pause_select_0.png, Pause_select_1.png, Pause_select_2.png
					for (int i = 0; i < 3; i++) {
						String[] names = new String[]{
								String.format("/ui/Pause_select_%d.png", i),
								String.format("/ui/pause_select_%d.png", i),
								String.format("/ui/PauseSelect_%d.png", i),
								String.format("/ui/pauseselect_%d.png", i)
						};
						for (String nm : names) {
							java.io.InputStream is3 = getClass().getResourceAsStream(nm);
							if (is3 != null) {
								try { iconSelectImgs[i] = ImageIO.read(is3); } catch (IOException ignored) {}
								break;
							}
						}
					}
				} catch (IOException ex) {
					// ignore
				}
		} catch (IOException ex) {
			generateImage();
		}
		setLayout(null);

		// update icon regions when resized (recompute scaled positions)
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				layoutButtons();
			}
		});

		// mouse handling for clicking icons and hover
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int idx = hitTest(e.getX(), e.getY());
				if (idx != hoverIndex) {
					hoverIndex = idx;
					setCursor(idx >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				hoverIndex = -1;
				setCursor(Cursor.getDefaultCursor());
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				int idx = hitTest(e.getX(), e.getY());
				if (idx >= 0 && listener != null) {
					if (idx == 0) listener.onResume();
					else if (idx == 1) listener.onSetting();
					else if (idx == 2) listener.onQuit();
				}
			}
		};
		addMouseListener(ma);
		addMouseMotionListener(ma);

		// setup keys for overlay (ESC to resume)
		setupKeyBindings();

		// consume mouse/keyboard events while visible
		setFocusable(true);
	}

	public void setPauseListener(PauseListener l) { this.listener = l; }

	/**
	 * Compute scaled icon regions based on the current component size and image.
	 * This will populate iconRegionsScaled[] using iconRegions[] (image coords).
	 */
	public void layoutButtons() {
		if (img == null) return;
		int w = getWidth();
		int h = getHeight();
		int iw = img.getWidth();
		int ih = img.getHeight();
		int maxW = (int) (w * 0.8);
		int maxH = (int) (h * 0.55);
		double scale = Math.min((double) maxW / iw, (double) maxH / ih);
		int dw = (int) (iw * scale);
		int dh = (int) (ih * scale);
		int x = (w - dw) / 2;
		int y = (h - dh) / 2 - 20;

		// ensure icon regions in image coords have been detected
		if (iconRegions[0] == null) detectIconRegionsInImage();

		for (int i = 0; i < 3; i++) {
			Rectangle r = iconRegions[i];
			if (r == null) {
				iconRegionsScaled[i] = null;
				continue;
			}
			int sx = x + (int) (r.x * scale);
			int sy = y + (int) (r.y * scale);
			int sw = Math.max(4, (int) (r.width * scale));
			int sh = Math.max(4, (int) (r.height * scale));
			iconRegionsScaled[i] = new Rectangle(sx, sy, sw, sh);
		}
		repaint();
	}

	private void setupKeyBindings() {
		InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = this.getActionMap();
		im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "resume");
		am.put("resume", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) listener.onResume();
			}
		});
	}

	/**
	 * Try to detect three icon regions inside the loaded image by scanning
	 * alpha/transparency in the central vertical band. If detection fails,
	 * fall back to dividing the image into three equal zones.
	 */
	private void detectIconRegionsInImage() {
		if (img == null) return;
		int w = img.getWidth();
		int h = img.getHeight();
		int top = h / 4;
		int bottom = (h * 3) / 4;

		boolean[] colHas = new boolean[w];
		for (int x = 0; x < w; x++) {
			boolean found = false;
			for (int y = top; y < bottom; y++) {
				int a = (img.getRGB(x, y) >>> 24) & 0xFF;
				if (a > 20) { found = true; break; }
			}
			colHas[x] = found;
		}

		java.util.List<int[]> runs = new java.util.ArrayList<>();
		int sx = -1;
		for (int x = 0; x < w; x++) {
			if (colHas[x]) {
				if (sx < 0) sx = x;
			} else {
				if (sx >= 0) { runs.add(new int[]{sx, x - 1}); sx = -1; }
			}
		}
		if (sx >= 0) runs.add(new int[]{sx, w - 1});

		// select up to 3 largest runs
		runs.sort((a,b) -> Integer.compare((b[1]-b[0]), (a[1]-a[0])));
		java.util.List<int[]> pick = runs.size() > 3 ? runs.subList(0,3) : runs;

		if (pick.size() < 3) {
			// fallback: equal thirds
			int part = w / 3;
			for (int i = 0; i < 3; i++) {
				int px = i * part;
				int pw = (i == 2) ? w - px : part;
				iconRegions[i] = new Rectangle(px, top, pw, bottom - top);
			}
			return;
		}

		// compute bounding boxes for each picked run
		// sort by x coordinate (left to right)
		pick.sort((a,b) -> Integer.compare(a[0], b[0]));
		for (int i = 0; i < 3; i++) {
			int[] r = pick.get(i);
			int lx = r[0], rx = r[1];
			int minY = h, maxY = 0;
			for (int x = lx; x <= rx; x++) {
				for (int y = top; y <= bottom; y++) {
					int a = (img.getRGB(x, y) >>> 24) & 0xFF;
					if (a > 20) {
						if (y < minY) minY = y;
						if (y > maxY) maxY = y;
					}
				}
			}
			if (minY > maxY) { // nothing found vertically
				minY = top; maxY = bottom;
			}
			iconRegions[i] = new Rectangle(lx, minY, rx - lx + 1, maxY - minY + 1);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (!isVisible()) return;
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// translucent backdrop
		g2.setColor(new Color(0, 0, 0, 160));
		g2.fillRect(0, 0, getWidth(), getHeight());

		// draw image (centered) and scale to a good visible size (bigger)
		if (img != null) {
			int iw = img.getWidth();
			int ih = img.getHeight();
			int maxW = (int) (getWidth() * 0.9);
			int maxH = (int) (getHeight() * 0.7);
			double scale = Math.min((double) maxW / iw, (double) maxH / ih);
			int dw = (int) (iw * scale);
			int dh = (int) (ih * scale);
			int x = (getWidth() - dw) / 2;
			int y = (getHeight() - dh) / 2 - 20;
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			// draw base image
			g2.drawImage(img, x, y, dw, dh, null);

			// ensure icon regions are computed
			if (iconRegions[0] == null) detectIconRegionsInImage();
			if (iconRegionsScaled[0] == null) layoutButtons();

				// if a per-icon select image is available, draw that scaled into the dst rect
				if (hoverIndex >= 0 && iconRegionsScaled[hoverIndex] != null) {
					Rectangle dst = iconRegionsScaled[hoverIndex];
					BufferedImage iconImg = iconSelectImgs[hoverIndex];
					if (iconImg != null) {
						g2.drawImage(iconImg, dst.x, dst.y, dst.x + dst.width, dst.y + dst.height,
								0, 0, iconImg.getWidth(), iconImg.getHeight(), null);
					} else if (imgSelect != null && iconRegions[hoverIndex] != null) {
						// fallback to drawing sub-region from the full select image
						Rectangle src = iconRegions[hoverIndex];
						int sx1 = src.x;
						int sy1 = src.y;
						int sx2 = src.x + src.width;
						int sy2 = src.y + src.height;
						g2.drawImage(imgSelect, dst.x, dst.y, dst.x + dst.width, dst.y + dst.height, sx1, sy1, sx2, sy2, null);
					}
				}
		}

		// no additional highlight; hover is represented by drawing imgSelect if available

		g2.dispose();
	}

	/**
	 * Run the pause overlay: show or hide the pause component.
	 */
	public void runPause(boolean show) {
		setVisible(show);
		if (show) {
			requestFocusInWindow();
		}
		repaint();
	}

	private void generateImage() {
		int w = 400, h = 180;
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig = bi.createGraphics();
		ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 10, 220), 0, h, new Color(30, 30, 30, 220));
		ig.setPaint(gp);
		ig.fillRoundRect(0, 0, w, h, 24, 24);

		ig.setColor(new Color(0, 255, 200));
		ig.fillRoundRect(8, 8, w - 16, 36, 12, 12);

		// decorative header bar only (no text)
		ig.setColor(new Color(0, 255, 200));
		ig.fillRoundRect(8, 8, w - 16, 36, 12, 12);

		ig.dispose();
		img = bi;
	}
}
