package com.bankstonks.ui;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

final class Collapsible extends JPanel
{
	private static final int DURATION_MS = 180;
	private static final int FRAME_MS = 15;

	private final JComponent content;
	private final Runnable onProgressChange;
	private boolean expanded;
	private double progress;
	private Timer timer;

	Collapsible(JComponent content, boolean expanded, Runnable onProgressChange)
	{
		super(null);
		this.content = content;
		this.onProgressChange = onProgressChange;
		this.expanded = expanded;
		this.progress = expanded ? 1 : 0;
		setOpaque(false);
		content.setVisible(expanded);
		add(content);
	}

	boolean isExpanded()
	{
		return expanded;
	}

	/** Eased, from 0 (closed) to 1 (open). */
	double progress()
	{
		return progress;
	}

	void setExpanded(boolean expanded, boolean animate)
	{
		if (this.expanded == expanded && (timer == null || !timer.isRunning()))
		{
			return;
		}
		this.expanded = expanded;
		if (timer != null)
		{
			timer.stop();
		}
		content.setVisible(true);
		if (!animate)
		{
			finish();
			return;
		}

		double from = progress;
		double to = expanded ? 1 : 0;
		long start = System.nanoTime();
		timer = new Timer(FRAME_MS, e ->
		{
			double t = fractionElapsedSince(start);
			progress = from + (to - from) * easeOutCubic(t);
			if (t >= 1)
			{
				((Timer) e.getSource()).stop();
				finish();
			}
			else
			{
				progressChanged();
			}
		});
		timer.start();
	}

	private static double fractionElapsedSince(long startNanos)
	{
		return Math.min(1, (System.nanoTime() - startNanos) / (DURATION_MS * 1_000_000.0));
	}

	private static double easeOutCubic(double t)
	{
		return 1 - Math.pow(1 - t, 3);
	}

	private void finish()
	{
		progress = expanded ? 1 : 0;
		content.setVisible(expanded);
		progressChanged();
	}

	private void progressChanged()
	{
		revalidate();
		repaint();
		if (onProgressChange != null)
		{
			onProgressChange.run();
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = content.getPreferredSize();
		return new Dimension(d.width, (int) Math.round(d.height * progress));
	}

	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(0, getPreferredSize().height);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	@Override
	public void doLayout()
	{
		// Full height and clipped by ours, so the content doesn't re-layout on every frame.
		content.setBounds(0, 0, getWidth(), content.getPreferredSize().height);
	}
}
