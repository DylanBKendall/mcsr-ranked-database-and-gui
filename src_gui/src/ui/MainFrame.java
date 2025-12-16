package ui;

import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("MCSR Ranked Analytics");

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Players", new PlayersPanel());
        tabs.addTab("Favorites", new FavoritesPanel());
        tabs.addTab("Matches", new MatchesPanel());
        tabs.addTab("Compare", new ComparePanel());
        tabs.addTab("Leaderboard", new LeaderboardPanel());

        setContentPane(tabs);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            MainFrame frame = new MainFrame();

            UiUtils.scaleUI(frame, 1.3f);

            frame.setVisible(true);
        });
    }
}