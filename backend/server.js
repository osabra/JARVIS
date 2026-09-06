import Fastify from "fastify";
import cors from "@fastify/cors";
import OpenAI from "openai";

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

const port = Number(process.env.PORT || 10000);

app.get("/", async () => ({
  name: "JARVIS Backend",
  status: "online"
}));

app.get("/health", async () => ({ status: "ok" }));

async function chatHandler(request, reply) {
  try {
    const key = process.env.OPENAI_API_KEY;
    if (!key) return reply.code(500).send({ error: "OPENAI_API_KEY is not configured" });

    const client = new OpenAI({ apiKey: key });
    const body = request.body || {};
    const messages = Array.isArray(body.messages) ? body.messages : [];
    const input = messages
      .filter(m => m && typeof m.content === "string")
      .map(m => ({ role: m.role === "assistant" ? "assistant" : "user", content: m.content }));

    if (!input.length) return reply.code(400).send({ error: "messages is required" });

    const response = await client.responses.create({
      model: process.env.OPENAI_MODEL || "gpt-5",
      instructions: "Eres JARVIS, un asistente personal avanzado. Responde en español salvo que el usuario pida otro idioma. Sé natural, útil y conciso.",
      input
    });

    return { id: response.id, text: response.output_text };
  } catch (error) {
    request.log.error(error);
    return reply.code(500).send({ error: "OpenAI request failed" });
  }
}

app.post("/v1/chat", chatHandler);
app.post("/v1/jarvis/chat", chatHandler);

await app.listen({ port, host: "0.0.0.0" });
