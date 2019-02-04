import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CarSellerAgent extends Agent {
    private int milleage;
    private int price;

    @Override
    protected void setup() {
        String[] init;
        Object[] args = getArguments();
        if (args != null && args.length > 0){
            init = ((String) args[0]).split(":");
            if (init.length == 2){
                milleage = Integer.parseInt(init[0]);
                price = Integer.parseInt(init[1]);
            }
        }

        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("car-selling");
        serviceDescription.setName("car-trading");
        agentDescription.addServices(serviceDescription);

        try{
            DFService.register(this, agentDescription);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(getAID().getName() + ":\t Terminating!");
    }

    private class OfferRequestsServer extends CyclicBehaviour{

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null){
                int targetMilleage = Integer.parseInt(msg.getContent());
                ACLMessage reply = msg.createReply();

                if (targetMilleage >= milleage){
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price));
                }
                else{
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else{
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour{

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null){
                int targetMilleage = Integer.parseInt(msg.getContent());
                ACLMessage reply = msg.createReply();

                if (targetMilleage >= milleage){
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(getAID().getName() + ":\t Car with milleage=" + milleage + " sold to agent: " + msg.getSender().getName());
                    doDelete();
                }
                else{
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else{
                block();
            }
        }
    }
}
