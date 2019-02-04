import jade.Boot;
import jade.core.Agent;

import java.util.LinkedList;
import java.util.Map;

public class Main {
    public static void main(String[] args){


        String agentsParam = String.join(";", new String[]{
                makeAgentString("seller1", CarSellerAgent.class, "1000:4900"),
                makeAgentString("seller2", CarSellerAgent.class, "650:5000"),
                makeAgentString("seller3", CarSellerAgent.class, "1000:4750"),
                makeAgentString("seller4", CarSellerAgent.class, "900:4600"),
                makeAgentString("seller5", CarSellerAgent.class, "4000:5100"),

                makeAgentString("buyer1", CarBuyerAgent.class, "800:5000"),
                makeAgentString("buyer2", CarBuyerAgent.class, "900:4500"),
                makeAgentString("buyer3", CarBuyerAgent.class, "1000:4200"),
                makeAgentString("buyer4", CarBuyerAgent.class, "700:4900"),
                makeAgentString("buyer5", CarBuyerAgent.class, "1500:5200")
        });
        System.out.println(agentsParam);

        Boot.main(new String[] {"-gui", "-agents", agentsParam});
    }

    private static String makeAgentsParam(Map<String, Class> agents){
        LinkedList<String> result = new LinkedList<String>();
        result.add("jade.Boot");
        agents.forEach((name, cls) -> result.add(makeAgentString(name, cls)));
        return String.join(";", result);
    }

    private static String makeAgentString(String name, Class agent){
        try {
            if (agent.newInstance() instanceof Agent) {
                return name + ':' + agent.getName();
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Param 'agent' must be instance of jade.core.Agent");
    }

    private static String makeAgentString(String name, Class agent, String args){
        try {
            if (agent.newInstance() instanceof Agent) {
                return name + ':' + agent.getName() + "(" + args + ")";
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Param 'agent' must be instance of jade.core.Agent");
    }
}