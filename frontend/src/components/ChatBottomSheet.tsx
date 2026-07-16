import {FormEvent, PointerEvent as ReactPointerEvent, useMemo, useRef, useState} from "react";
import {chat} from "../api";
import type {ChatMessage} from "../types";

interface Props {
    context: unknown;
}

const CLOSED = 82;
export default function ChatBottomSheet({context}: Props) {
    const max = useMemo(() => Math.round(window.innerHeight * .88), []);
    const half = useMemo(() => Math.round(window.innerHeight * .54), []);
    const [height, setHeight] = useState(CLOSED);
    const [drag, setDrag] = useState<{ y: number; height: number }>();
    const [input, setInput] = useState("");
    const [loading, setLoading] = useState(false);
    const [suggestions, setSuggestions] = useState(["비 안 맞는 코스로 바꿔줘", "덜 걷는 일정으로 바꿔줘", "숨은 명소를 추가해줘", "지금 주변 맛집 추천해줘"]);
    const [messages, setMessages] = useState<ChatMessage[]>([{
        id: crypto.randomUUID(),
        role: "assistant",
        content: "안녕하세요. 현재 위치와 여행 경로를 바탕으로 일정을 함께 조정해드릴게요.",
        createdAt: new Date().toISOString()
    }]);
    const scroll = useRef<HTMLDivElement | null>(null);
    const open = height > CLOSED + 20;

    function snap(h: number) {
        const points = [CLOSED, half, max];
        setHeight(points.reduce((a, b) => Math.abs(b - h) < Math.abs(a - h) ? b : a));
    }

    function down(e: ReactPointerEvent<HTMLDivElement>) {
        e.currentTarget.setPointerCapture(e.pointerId);
        setDrag({y: e.clientY, height});
    }

    function move(e: ReactPointerEvent<HTMLDivElement>) {
        if (!drag) return;
        setHeight(Math.max(CLOSED, Math.min(max, drag.height + drag.y - e.clientY)));
    }

    function up() {
        snap(height);
        setDrag(undefined);
    }

    async function submit(text: string) {
        const value = text.trim();
        if (!value || loading) return;
        if (!open) setHeight(half);
        const user: ChatMessage = {
            id: crypto.randomUUID(),
            role: "user",
            content: value,
            createdAt: new Date().toISOString()
        };
        const history = [...messages, user];
        setMessages(history);
        setInput("");
        setLoading(true);
        try {
            const response = await chat(value, history, context);
            setMessages(v => [...v, {
                id: crypto.randomUUID(),
                role: "assistant",
                content: response.message,
                createdAt: new Date().toISOString()
            }]);
            if (response.suggestedActions?.length) setSuggestions(response.suggestedActions.slice(0, 4));
            setTimeout(() => scroll.current?.scrollTo({top: scroll.current.scrollHeight, behavior: "smooth"}), 30);
        } catch (e) {
            setMessages(v => [...v, {
                id: crypto.randomUUID(),
                role: "assistant",
                content: e instanceof Error ? `연결 오류: ${e.message}` : "AI 연결 오류",
                createdAt: new Date().toISOString()
            }]);
        } finally {
            setLoading(false);
        }
    }

    function onSubmit(e: FormEvent) {
        e.preventDefault();
        void submit(input);
    }

    return <section className={`chat-sheet ${open ? "open" : ""}`} style={{height}}>
        <div className="chat-drag-zone" onPointerDown={down} onPointerMove={move} onPointerUp={up} onPointerCancel={up}>
            <span className="drag-handle"/>
            <button className="chat-peek" onClick={() => setHeight(open ? CLOSED : half)}><span
                className="chat-orb">✦</span><span><strong>ROAMATE AI</strong><small>{open ? "끌어내리면 대화방이 닫혀요" : "누르거나 끌어올려 대화를 시작하세요"}</small></span><b>{open ? "⌄" : "⌃"}</b>
            </button>
        </div>
        <div className="chat-expanded">
            <header>
                <div><span className="chat-orb large">✦</span>
                    <div><strong>ROAMATE AI 여행 비서</strong><small>현재 위치·장소·경로를 함께 이해합니다</small></div>
                </div>
                <button onClick={() => setHeight(CLOSED)}>×</button>
            </header>
            <div className="chat-messages" ref={scroll}>{messages.map(m => <article className={`chat-message ${m.role}`}
                                                                                    key={m.id}>{m.role === "assistant" &&
                <span className="message-avatar">✦</span>}<p>{m.content}</p></article>)}{loading &&
                <article className="chat-message assistant"><span className="message-avatar">✦</span><p
                    className="typing"><i/><i/><i/></p></article>}</div>
            <div className="chat-suggestions">{suggestions.map(s => <button key={s}
                                                                            onClick={() => void submit(s)}>{s}</button>)}</div>
            <form className="chat-input" onSubmit={onSubmit}><input value={input}
                                                                    onChange={e => setInput(e.target.value)}
                                                                    placeholder="여행 상황이나 원하는 변화를 말해주세요"/>
                <button disabled={loading}>↑</button>
            </form>
        </div>
    </section>;
}
