import Fastify from "fastify";
import cors from "@fastify/cors";
import OpenAI from "openai";

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

const port = Number(process.env.PORT || 8080);
const apiKey = process.env.OPENAI_API_KEY;
if (!apiKey) throw new Error("OPENAI_API_KEY is required on the server");

const client = new OpenAI({ apiKey });

app.get("/health", async () => ({ ok: true, service: "JARVIS", provider: "OpenAI" }));

app.post("/v1/jarvis/chat", async (request, reply) => {
  const body = request.body || {};
  const messages = Array.isArray(body.messages) ? body.messages : [];
  const previousResponseId = typeof body.previous_response_id === "string" ? body.previous_response_id : undefined;

  if (!messages.length && !previousResponseId) {
    return reply.code(400).send({ error: "messages or previous_response_id is required" });
  }

  const input = messages.slice(-30).map((m) => ({
    role: m.role === "assistant" ? "assistant" : "user",
    content: String(m.content || "")
  }));

  const response = await client.responses.create({
    model: "gpt-5",
    previous_response_id: previousResponseId,
    instructions: "Eres JARVIS, un asistente personal en español. Habla de forma natural, clara, útil y segura. Si el usuario te llama JARVIS, responde como su asistente. No afirmes haber ejecutado una acción del teléfono si el dispositivo no la confirmó.",
    input,
    tools: [{ type: "web_search" }]
  });

  return {
    id: response.id,
    text: response.output_text || "No tengo una respuesta en este momento."
  };
});

app.listen({ port, host: "0.0.0.0" });
