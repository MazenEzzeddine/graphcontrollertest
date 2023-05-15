import graph.Graph;
import graph.Vertex;
import group.ConsumerGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;


public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        initialize();
    }

    private static void initialize() throws InterruptedException, ExecutionException {
        Graph g = new Graph(4);

      /*  ConsumerGroup g0 = new ConsumerGroup("testtopic1", 1,
                175, 2,
                "security", "testgroup1");
        ConsumerGroup g1 = new ConsumerGroup("testtopic2", 1,
                175, 2,
                "visa", "testgroup2");
        ConsumerGroup g2 = new ConsumerGroup("testtopic3", 1,
                175, 2,
                "merchant", "testgroup3");
        ConsumerGroup g3 = new ConsumerGroup("testtopic4", 1,
                175, 2,
                "client", "testgroup4");
*/

        ConsumerGroup g0 = new ConsumerGroup("testtopic1", 1,85,  0.5,
                "security", "testgroup1");
        ConsumerGroup g1 = new ConsumerGroup("testtopic2", 1, 85,0.5,
                "merchant", "testgroup2");
        ConsumerGroup g2 = new ConsumerGroup("testtopic3", 1, 85,  0.5,
                "client", "testgroup3");
/*        ConsumerGroup g3 = new ConsumerGroup("testtopic4", 1, 1.66,
                "client", "testgroup4");*/

        g.addVertex(0, g0);
        g.addVertex(1, g1);
        g.addVertex(2, g2);
        //g.addVertex(3, g3);

        g.addEdge(0, 1);
        g.addEdge(1, 2);
       /* g.addEdge(1, 3);
        g.addEdge(2, 3);*/

        ////////////////////////////////////

        Stack<Vertex> ts = g.dfs(g.getVertex(0));
        List<Vertex> topoOrder = new ArrayList<>();
        //topological order
        while (!ts.isEmpty()) {
            topoOrder.add(ts.pop());
        }
         log.info("The graph in topo order");
        for (int i = 0; i < topoOrder.size(); i++) {
            log.info("vertex index {}, vertex name {} ", i, topoOrder.get(i).getG().getName());
        }

/*
        log.info("Warming for 2 minutes seconds.");
        Thread.sleep(60*2*1000);*/
        log.info("Warming 30  seconds.");
        Thread.sleep(30 * 1000);

        while (true) {
            log.info("Querying Prometheus");
            Main.QueryingPrometheus(g, topoOrder);
            log.info("Sleeping for 5 seconds");
            log.info("******************************************");
            log.info("******************************************");
            Thread.sleep(5000);
        }
    }


    static void QueryingPrometheus(Graph g, List<Vertex> topoOrder)
            throws ExecutionException, InterruptedException {

        ArrivalRates.arrivalRateTopicGeneral(g.getVertex(0).getG(), false);
        ArrivalRates.arrivalRateTopicGeneral(g.getVertex(1).getG(), false);
        ArrivalRates.arrivalRateTopicGeneral(g.getVertex(2).getG(), false);


        if (Duration.between(topoOrder.get(0).getG().getLastUpScaleDecision(),
                Instant.now()).getSeconds() > 10) {
            //queryconsumergroups.QueryRate.queryConsumerGroup();
            BinPack2.scaleAsPerBinPack(topoOrder.get(0).getG());
        }

        if (Duration.between(topoOrder.get(1).getG().getLastUpScaleDecision(),
                Instant.now()).getSeconds() > 10) {
            //queryconsumergroups.QueryRate.queryConsumerGroup();
            BinPack2.scaleAsPerBinPack(topoOrder.get(1).getG());
        }

        if (Duration.between(topoOrder.get(2).getG().getLastUpScaleDecision(),
                Instant.now()).getSeconds() > 10) {
            //queryconsumergroups.QueryRate.queryConsumerGroup();
            BinPack2.scaleAsPerBinPack(topoOrder.get(2).getG());
        }

       // Util.computeBranchingFactors(g);
       /* for (int m = 0; m < topoOrder.size(); m++) {
            log.info("Vertex/CG number {} in topo order is {}", m, topoOrder.get(m).getG().getName());
            getArrivalRate(g, m);
            if (Duration.between(topoOrder.get(m).getG().getLastUpScaleDecision(),
                    Instant.now()).getSeconds() > 15) {
                //queryconsumergroups.QueryRate.queryConsumerGroup();
                BinPack2.scaleAsPerBinPack(topoOrder.get(m).getG());
            }
        }*/
    }


    static void getArrivalRate(Graph g, int m) throws ExecutionException, InterruptedException {

        int[][] A = g.getAdjMat();

        boolean grandParent = true;
        double totalArrivalRate = 0.0;
        for (int parent = 0; parent < A[m].length; parent++) {
            if (A[parent][m] == 1) {
                //log.info( " {} {} is a prarent of {} {}", parent, g.getVertex(parent).getG() , m, g.getVertex(m).getG() );
                grandParent = false;
                totalArrivalRate += (g.getVertex(parent).getG().getTotalArrivalRate() /*+
                        (g.getVertex(parent).getG().getTotalLag()/(g.getVertex(parent).getG().getWsla()))*/)
                        * g.getBF()[parent][m];
            }
        }

        if (grandParent) {
            ArrivalRates.arrivalRateTopicGeneral(g.getVertex(m).getG(), false);
            log.info("Arrival rate of micorservice {} {} {}", m, g.getVertex(m).getG().getName(),
                    g.getVertex(m).getG().getTotalArrivalRate());
        } else {
            g.getVertex(m).getG().setTotalArrivalRate(totalArrivalRate);
//            ArrivalRates.arrivalRateTopicGeneral(g.getVertex(m).getG(), true);
            log.info("Arrival rate of micorservice {} {} {}", m,g.getVertex(m).getG().getName()
                    , g.getVertex(m).getG().getTotalArrivalRate());
        }

    }

}
