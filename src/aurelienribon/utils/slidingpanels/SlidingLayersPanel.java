package aurelienribon.utils.slidingpanels;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.utils.Animator;
import aurelienribon.utils.slidingpanels.SlidingLayersConfig.Tile;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLayeredPane;
import javax.swing.Timer;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class SlidingLayersPanel extends JLayeredPane {
	private final List<Keyframe> timeline = new ArrayList<Keyframe>();
	private int timelineIdx;
	private Timer resizeTimer;
	private TweenManager tweenManager;

	public static interface Callback {
		public void done();
	}

	public SlidingLayersPanel() {
		addComponentListener(new ComponentAdapter() {
			private boolean firstResize = true;

			@Override
			public void componentResized(ComponentEvent e) {
				if (timeline != null && firstResize) {
					Keyframe kf = timeline.get(timelineIdx);
					kf.cfg.placeAndRoute();
					place(kf);
				} else if (timeline != null) {
					if (resizeTimer != null) resizeTimer.stop();
					resizeTimer = new Timer(300, new ActionListener() {
						@Override public void actionPerformed(ActionEvent e) {
							Keyframe kf = timeline.get(timelineIdx);
							kf.cfg.placeAndRoute();
							tween(kf, false);
						}
					});
					resizeTimer.setRepeats(false);
					resizeTimer.start();
				}

				firstResize = false;
			}
		});
	}

	public void setTweenManager(TweenManager tweenManager) {
		this.tweenManager = tweenManager;
	}

	public SlidingLayersPanel timeline() {
		timeline.clear();
		return this;
	}

	public SlidingLayersPanel play() {
		timelineIdx = 0;
		go(timeline.get(0));
		return this;
	}

	public SlidingLayersPanel pushSet(SlidingLayersConfig cfg) {
		Keyframe kf = new Keyframe();
		kf.animate = false;
		kf.cfg = cfg;
		timeline.add(kf);
		return this;
	}

	public SlidingLayersPanel pushTo(SlidingLayersConfig cfg) {
		Keyframe kf = new Keyframe();
		kf.animate = true;
		kf.cfg = cfg;
		timeline.add(kf);
		return this;
	}

	public SlidingLayersPanel setCallback(Callback callback) {
		timeline.get(timeline.size()-1).callback = callback;
		return this;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static class Keyframe {
		public boolean animate;
		public SlidingLayersConfig cfg;
		public Callback callback;
	}

	private void go(Keyframe kf) {
		kf.cfg.placeAndRoute();

		List<Component> currentCmps = Arrays.asList(getComponents());
		for (Component c : kf.cfg.getComponents()) {
			if (!currentCmps.contains(c)) {
				Tile t = kf.cfg.getTile(c);
				c.setBounds(t.x, t.y, t.w, t.h);
			}
		}

		removeAll();
		for (Component c : kf.cfg.getComponents()) {
			add(c, new Integer(1));
		}

		if (kf.animate) tween(kf, true);
		else place(kf);
	}

	private void tween(final Keyframe kf, boolean useDelays) {
		tweenManager.killAll();

		Timeline tl = Timeline.createParallel();

		for (Component c : kf.cfg.getComponents()) {
			Tile t = kf.cfg.getTile(c);

			int x = c.getX();
			int y = c.getY();
			int w = c.getWidth();
			int h = c.getHeight();
			int dx = x - t.x;
			int dy = y - t.y;
			int dw = w - t.w;
			int dh = h - t.h;
			boolean animXY = (dx != 0) || (dy != 0);
			boolean animWH = (dw != 0) || (dh != 0);
			float dxy = (float) Math.sqrt(dx*dx + dy*dy);
			float dwh = (float) Math.sqrt(dw*dw + dh*dh);
			float duration = Math.max(0.4f, Math.max(dxy, dwh) * 0.12f / 100);

			if (animXY && animWH) {
				tl.push(Tween.to(c, Animator.JComponentAccessor.XYWH, duration)
					.target(t.x, t.y, t.w, t.h)
					.delay(useDelays ? t.delay : 0)
				);
			} else if (animXY) {
				tl.push(Tween.to(c, Animator.JComponentAccessor.XY, duration)
					.target(t.x, t.y)
					.delay(useDelays ? t.delay : 0)
				);
			} else if (animWH) {
				tl.push(Tween.to(c, Animator.JComponentAccessor.WH, duration)
					.target(t.w, t.h)
					.delay(useDelays ? t.delay : 0)
				);
			}
		}

		tl.setCallback(new TweenCallback() {
			@Override public void onEvent(int type, BaseTween<?> source) {
				if (timelineIdx < timeline.size()-1) {
					if (kf.callback != null) kf.callback.done();
					timelineIdx++;
					go(timeline.get(timelineIdx));
				}
			}
		});

		tl.start(tweenManager);
	}

	private void place(Keyframe kf) {
		for (Component c : kf.cfg.getComponents()) {
			Tile t = kf.cfg.getTile(c);
			c.setBounds(t.x, t.y, t.w, t.h);
		}

		if (timelineIdx < timeline.size()-1) {
			if (kf.callback != null) kf.callback.done();
			timelineIdx++;
			go(timeline.get(timelineIdx));
		}
	}
}