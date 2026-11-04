import {useEffect, useState} from "react";
import {AssistantService, BookingService} from "Frontend/generated/endpoints";
import BookingDetails from "../generated/org/vaadin/marcus/service/BookingDetails";
import {GridColumn} from "@vaadin/react-components/GridColumn";
import {Grid} from "@vaadin/react-components/Grid";
import {MessageInput} from "@vaadin/react-components/MessageInput";
import {nanoid} from "nanoid";
import {SplitLayout} from "@vaadin/react-components/SplitLayout";
import Message, {MessageItem} from "../components/Message";
import MessageList from "Frontend/components/MessageList";

type Ticket = {id: string; bookingNumber: string; status: string; proposedAction?: string; recommendation?: string};

export default function Index() {
  const [chatId, setChatId] = useState(nanoid());
  const [working, setWorking] = useState(false);
  const [bookings, setBookings] = useState<BookingDetails[]>([]);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [operatorKey, setOperatorKey] = useState("");
  const [ticketError, setTicketError] = useState("");
  const [messages, setMessages] = useState<MessageItem[]>([{
    role: 'assistant',
    content: 'Welcome to Funnair! How can I help you?'
  }]);

  useEffect(() => {
    // Update bookings when we have received the full response
    if (!working) {
      BookingService.getBookings().then(setBookings);
    }
  }, [working]);

  async function loadTickets() {
    if (!operatorKey) return;
    const response = await fetch('/api/tickets', {headers: {'X-Operator-Key': operatorKey}});
    if (!response.ok) { setTicketError('Cannot load tickets. Check operator key.'); return; }
    setTicketError('');
    setTickets(await response.json());
  }

  async function decide(id: string, decision: 'approve' | 'reject') {
    const response = await fetch(`/api/tickets/${id}/${decision}`, {
      method: 'POST', headers: {'X-Operator-Key': operatorKey}
    });
    if (!response.ok) { setTicketError(`Could not ${decision} ticket.`); return; }
    await loadTickets();
    BookingService.getBookings().then(setBookings);
  }

  function addMessage(message: MessageItem) {
    setMessages(messages => [...messages, message]);
  }

  function appendToLatestMessage(chunk: string) {
    setMessages(messages => {
      const latestMessage = messages[messages.length - 1];
      latestMessage.content += chunk;
      return [...messages.slice(0, -1), latestMessage];
    });
  }

  async function sendMessage(message: string) {
    setWorking(true);
    addMessage({
      role: 'user',
      content: message
    });
    let first = true;
    AssistantService.chat(chatId, message)
      .onNext(token => {
        if (first && token) {
          addMessage({
            role: 'assistant',
            content: token
          });

          first = false;
        } else {
          appendToLatestMessage(token);
        }
      })
      .onError(() => setWorking(false))
      .onComplete(() => setWorking(false));
  }


  return (
    <SplitLayout className="h-full">
      <div className="flex flex-col gap-m p-m box-border h-full" style={{width: '30%'}}>
        <h3>Funnair support</h3>
        <MessageList messages={messages} className="flex-grow overflow-scroll"/>
        <MessageInput onSubmit={e => sendMessage(e.detail.value)} className="px-0"/>
      </div>
      <div className="flex flex-col gap-m p-m box-border" style={{width: '70%'}}>
        <h3>Bookings database</h3>
        <Grid items={bookings} className="flex-shrink-0">
          <GridColumn path="bookingNumber" autoWidth header="#"/>
          <GridColumn path="firstName" autoWidth/>
          <GridColumn path="lastName" autoWidth/>
          <GridColumn path="date" autoWidth/>
          <GridColumn path="from" autoWidth/>
          <GridColumn path="to" autoWidth/>
          <GridColumn path="bookingStatus" autoWidth header="Status">
            {({item}) => item.bookingStatus === "CONFIRMED" ? "✅" : "❌"}
          </GridColumn>
          <GridColumn path="bookingClass" autoWidth/>
        </Grid>
        <h3>Support tickets</h3>
        <div className="flex gap-s">
          <input type="password" placeholder="Operator key" value={operatorKey}
                 onChange={e => setOperatorKey(e.target.value)}/>
          <button onClick={loadTickets}>Refresh tickets</button>
        </div>
        {ticketError && <p role="alert">{ticketError}</p>}
        <div className="overflow-scroll">
          {tickets.map(ticket => <div key={ticket.id} className="p-s border-b">
            <strong>{ticket.bookingNumber}</strong> · {ticket.status} · {ticket.proposedAction || 'No action'}
            <p>{ticket.recommendation}</p>
            {ticket.status === 'PENDING_APPROVAL' && <div className="flex gap-s">
              <button onClick={() => decide(ticket.id, 'approve')}>Approve</button>
              <button onClick={() => decide(ticket.id, 'reject')}>Reject</button>
            </div>}
          </div>)}
        </div>
      </div>
    </SplitLayout>

  );
}
