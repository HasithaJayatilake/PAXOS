import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IntegratedPaxosTester {
    private final CyclicBarrier gate;
    ThreadGroup threadGroup;
    LinkedList<Peer> peerList;
    File file1;
    File file2;
    File file3;
    Peer M1;
    Peer M2;
    Peer M3;
    Peer M4;
    Peer M5;
    Peer M6;
    Peer M7;
    Peer M8;
    Peer M9;

    public static void main(String[] args) {
        IntegratedPaxosTester tester = new IntegratedPaxosTester();
        try {
            tester.runIntegratedTester();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IntegratedPaxosTester() {
        this.gate = new CyclicBarrier(10);
        this.file1 = new File("expected-output-1.txt");
        this.file2 = new File("expected-output-2.txt");
        this.file3 = new File("expected-output-3.txt");
        this.peerList = new LinkedList<Peer>();
    }

    public void runIntegratedTester() throws Exception {

        // Instantiating useful variables
        BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
        final String ipAddress = "230.0.0.0";
        ;
        final InetAddress group = InetAddress.getByName(ipAddress);
        final int port = 4567;
        // Create server object
        Server server = new Server(group, port);

        boolean run = true;

        while (run) {

            // if (this.threadGroup != null) {
            // System.out.println("Active threadcount = " + this.threadGroup.activeCount());
            // }

            System.out.println("\nWelcome to the Integrated Tester!\n");
            System.out.println("Each testing option below is a sequence of proposals");
            System.out.println(
                    "A diff function compares the decided paxos values of each node, \n with the expected output at the end of the series of proposals\n");
            System.out.println("Choose a testing option below\n");

            System.out.println("1) One proposer at a time no failures (M1 proposes initially, then M2 proposes)");
            System.out.println("2) One proposer at a time no failures (M1 proposes initially, once complete M2 proposes twice)");
            System.out.println("3) One proposer with failures (M2 proposes then dies)");
            System.out.println("4) Two proposers without failures (M1 and M2 propose together)");
            System.out.println("5) Two proposers with failures (M1 and M2 propose together, then M2 dies)");
            System.out.println("6) Exit\n");
            System.out.print("Option number: ");

            try {
                int userInput = Integer.parseInt(userInputReader.readLine());
                File outputFile = new File("output.txt");
                FileWriter filewriter = new FileWriter(outputFile, false);
                BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
                PrintWriter outputWriter = new PrintWriter(bufferedwriter);
                switch (userInput) {
                    case 1:
                        Init_Peers(false);
                        M1.sendProposal(false);
                        while (true) {
                            if (M1.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        M2.sendProposal(false);
                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        TimeUnit.SECONDS.sleep(2);
                        for (Peer peer : peerList) {
                            outputWriter.println(peer.username + ": president-" + peer.president);
                        }
                        outputWriter.close();

                        if (filesDiff(this.file1, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 1 successful!");
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 1 failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }

                        System.exit(0);
                        break;
                    case 2:
                        Init_Peers(false);
                        M1.sendProposal(false);
                        while (true) {
                            if (M1.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        M2.sendProposal(false);
                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        TimeUnit.SECONDS.sleep(2);

                        M2.sendProposal(false);
                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        TimeUnit.SECONDS.sleep(2);
                        for (Peer peer : peerList) {
                            outputWriter.println(peer.username + ": president-" + peer.president);
                        }
                        outputWriter.close();

                        if (filesDiff(this.file2, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 2 successful!");
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 2 failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }

                        System.exit(0);
                        break;
                    case 3:
                        Init_Peers(true);
                        M2.sendProposal(false);
                        // M2.getListener().interrupt();
                        M2.interrupt();
                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase2)) {
                                break;
                            }
                        }

                        TimeUnit.SECONDS.sleep(1);

                        for (Peer peer : peerList) {
                            outputWriter.println(peer.username + ": president-" + peer.president);
                        }
                        outputWriter.close();

                        if (filesDiff(this.file3, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 3 successful!");
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 3 failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }
                        System.exit(0);
                        break;
                    case 4:
                        Init_Peers(false);
                        M1.sendProposal(false);
                        M2.sendProposal(false);

                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        TimeUnit.SECONDS.sleep(1);

                        for (Peer peer : peerList) {
                            outputWriter.println(peer.username + ": president-" + peer.president);
                        }
                        outputWriter.close();

                        if (filesDiff(this.file2, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 4 successful!");
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 4 failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }
                        System.exit(0);
                        break;
                    case 5:
                        Init_Peers(true);
                        M1.sendProposal(false);
                        M2.sendProposal(false);

                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase2)) {
                                break;
                            }
                        }

                        TimeUnit.SECONDS.sleep(1);

                        for (Peer peer : peerList) {
                            outputWriter.println(peer.username + ": president-" + peer.president);
                        }
                        outputWriter.close();

                        if (filesDiff(this.file3, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 5 successful!");
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case 5 failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }

                        System.exit(0);
                        break;

                    case 6:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid input! Try again");
                        outputWriter.close();
                        throw new IOException();
                }

            } catch (Exception e) {
                // e.printStackTrace();
                System.out.println("\n------------------------------\n");
                continue;
            }
        }
    }

    public void Init_Peers(boolean failureMode) {
        this.threadGroup = new ThreadGroup("Paxos-group");

        this.M1 = new Peer("M1", threadGroup, false);
        if (failureMode) {
            this.M2 = new Peer("M2", threadGroup, true);
        } else {
            this.M2 = new Peer("M2", threadGroup, false);
        }
        this.M3 = new Peer("M3", threadGroup, false);
        this.M4 = new Peer("M4", threadGroup, false);
        this.M5 = new Peer("M5", threadGroup, false);
        this.M6 = new Peer("M6", threadGroup, false);
        this.M7 = new Peer("M7", threadGroup, false);
        this.M8 = new Peer("M8", threadGroup, false);
        this.M9 = new Peer("M9", threadGroup, false);

        this.peerList.clear();
        this.peerList.add(M1);
        this.peerList.add(M2);
        this.peerList.add(M3);
        this.peerList.add(M4);
        this.peerList.add(M5);
        this.peerList.add(M6);
        this.peerList.add(M7);
        this.peerList.add(M8);
        this.peerList.add(M9);

        M1.start();
        M2.start();
        M3.start();
        M4.start();
        M5.start();
        M6.start();
        M7.start();
        M8.start();
        M9.start();
    }

    public boolean isProtocolRunning() {
        boolean isRunning = false;
        for (Peer peer : peerList) {
            boolean isComplete = peer.getListener().getAcceptorPhase().equals(PeerThread.phase3);
            if (!isComplete) {
                System.out.println("Paxos still running");
                isRunning = true;
            }
        }

        return isRunning;
    }

    public boolean filesDiff(File f1, File f2) {

        boolean diff = true;

        try {
            BufferedReader reader1 = new BufferedReader(new FileReader(f1));
            BufferedReader reader2 = new BufferedReader(new FileReader(f2));

            String line1 = null;
            String line2 = null;
            while ((diff) && ((line1 = reader1.readLine()) != null) && ((line2 = reader2.readLine()) != null)) {
                if (!line1.equals(line2)) {
                    diff = false;
                    break;
                }
            }
            reader1.close();
            reader2.close();
            return diff;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
