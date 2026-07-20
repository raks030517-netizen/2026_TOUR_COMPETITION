import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { chat } from "../api";
import type { ChatMessage } from "../types";

interface Props {
  context: unknown;
  onAdjustment?: (message: string) => Promise<string | undefined>;
}

const INITIAL_SUGGESTIONS = ["비가 오면 실내 코스로 바꿔줘", "지금 근처 카페를 추천해줘", "걷는 시간을 줄여줘", "야경 명소를 추가해줘"];

function shouldAdjustPlan(message: string): boolean {
  return /비|실내|우산|걷|피곤|택시|줄여|카페|휴식|야경|밤|노을|추가|바꿔|변경/.test(message);
}

export default function ChatBottomSheet({ context, onAdjustment }: Props) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [suggestions, setSuggestions] = useState(INITIAL_SUGGESTIONS);
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: "welcome", role: "assistant", content: "안녕하세요. 저는 부산 여행을 함께 조율하는 ROAMATE예요. 일정이나 지금 상황을 편하게 말해 주세요.", createdAt: new Date().toISOString() },
  ]);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, loading]);

  const send = async (text: string) => {
    const value = text.trim();
    if (!value || loading) return;

    const userMessage: ChatMessage = { id: crypto.randomUUID(), role: "user", content: value, createdAt: new Date().toISOString() };
    const history = [...messages, userMessage];
    setMessages(history);
    setInput("");
    setOpen(true);
    setLoading(true);

    try {
      const adjustment = shouldAdjustPlan(value) && onAdjustment
        ? await onAdjustment(value)
        : undefined;
      const response = await chat(value, history, context);
      const content = adjustment ? `${adjustment}\n\n${response.message}` : response.message;
      setMessages((current) => [...current, { id: crypto.randomUUID(), role: "assistant", content, createdAt: new Date().toISOString() }]);
      if (response.suggestedActions.length) setSuggestions(response.suggestedActions.slice(0, 4));
    } catch (caughtError) {
      const detail = caughtError instanceof Error ? caughtError.message : "연결 오류";
      setMessages((current) => [...current, { id: crypto.randomUUID(), role: "assistant", content: `AI 메이트에 연결하지 못했어요. (${detail}) 잠시 후 다시 시도해 주세요.`, createdAt: new Date().toISOString() }]);
    } finally {
      setLoading(false);
    }
  };

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void send(input);
  };

  return (
    <section className={`chat-sheet ${open ? "open" : ""}`} style={{ height: open ? "min(560px, 78vh)" : 82 }}>
      <div className="chat-drag-zone">
        <span className="drag-handle" />
        <button type="button" className="chat-peek" onClick={() => setOpen((value) => !value)} aria-expanded={open}>
          <span className="chat-orb">✦</span><span><strong>ROAMATE AI 여행 메이트</strong><small>{open ? "대화를 접으려면 눌러 주세요" : "일정 변경이나 추천이 필요할 때 물어보세요"}</small></span><b>{open ? "⌄" : "⌃"}</b>
        </button>
      </div>
      {open && <div className="chat-expanded">
        <div className="chat-messages" ref={scrollRef}>
          {messages.map((message) => <article className={`chat-message ${message.role}`} key={message.id}>{message.role === "assistant" && <span className="message-avatar">✦</span>}<p>{message.content}</p></article>)}
          {loading && <article className="chat-message assistant"><span className="message-avatar">✦</span><p className="typing"><i /><i /><i /></p></article>}
        </div>
        <div className="chat-suggestions">{suggestions.map((suggestion) => <button type="button" key={suggestion} onClick={() => void send(suggestion)} disabled={loading}>{suggestion}</button>)}</div>
        <form className="chat-input" onSubmit={submit}><input value={input} onChange={(event) => setInput(event.target.value)} placeholder="여행 상황이나 원하는 변화를 말해 주세요" disabled={loading} /><button type="submit" disabled={!input.trim() || loading} aria-label="메시지 보내기">↑</button></form>
      </div>}
    </section>
  );
}
