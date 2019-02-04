import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CarBuyerAgent extends Agent {
    private int targetMelleage;
    private int maxPrice;
    private AID[] sellerAgents;

    @Override
    protected void setup() {
        System.out.println(getAID().getName() + ":\t Started!");
        String[] carInfo;
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            carInfo = ((String) args[0]).split(":");
            if (carInfo.length >= 2) {
                targetMelleage = Integer.parseInt(carInfo[0]);
                maxPrice = Integer.parseInt(carInfo[1]);
                addBehaviour(new TickerBehaviour(this, 15000) {
                    @Override
                    protected void onTick() {
                        System.out.println();
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription serviceDescription = new ServiceDescription();
                        serviceDescription.setType("car-selling");
                        template.addServices(serviceDescription);
                        try {
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            System.out.println(getAID().getName() + ":\t Found the following seller agents!");
                            sellerAgents = new AID[result.length];
                            for (int i = 0; i < result.length; ++i) {
                                sellerAgents[i] = result[i].getName();
                            }
                        } catch (FIPAException fe) {
                            fe.printStackTrace();
                        }
                        myAgent.addBehaviour(new RequestPerformer());
                    }
                });
            }
        } else {
            System.out.println(getAID().getName() + ":\t No car info in args!");
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(getAID().getName() + ":\t Terminating!");
    }

    private class RequestPerformer extends Behaviour{
        private AID bestSeller;
        private int bestPrice;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        @Override
        public void action() {
            switch (step){
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i){
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(String.valueOf(targetMelleage));
                    cfp.setConversationId("car-trade");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("car-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null){
                        if (reply.getPerformative() == ACLMessage.PROPOSE){
                            int price = Integer.parseInt(reply.getContent());
                            if (price <= maxPrice && (bestSeller == null || price < bestPrice)){
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length){
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(String.valueOf(targetMelleage));
                    order.setConversationId("car-trade");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("car-trade"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    reply = myAgent.receive(mt);
                    if (reply != null){
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println(myAgent.getAID().getName() + ":\t Successfully purchased from agent " + reply.getSender().getName() + " for " + bestPrice);
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println(myAgent.getAID().getName() + ":\t Attempt failed: requested car already sold.");
                        }
                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: car with milleage "+targetMelleage+" not available for sale");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }
}
