import express from 'express';
import cors from 'cors';
import multer from 'multer';
import RunwayML, { TaskFailedError } from '@runwayml/sdk';

const app = express();
const upload = multer({ limits: { fileSize: 5 * 1024 * 1024 } });
app.use(cors());
app.use(express.json({ limit: '1mb' }));

const port = process.env.PORT || 8080;
const runwayKey = process.env.RUNWAYML_API_SECRET;
if (!runwayKey) console.warn('RUNWAYML_API_SECRET is not configured.');

const client = runwayKey ? new RunwayML({ apiKey: runwayKey }) : null;

app.get('/health', (_req, res) => res.json({ ok: true, service: 'jarvis-video', runwayConfigured: !!client }));

app.post('/v1/image-to-video', upload.single('image'), async (req, res) => {
  try {
    if (!client) return res.status(503).json({ error: 'Runway API no configurada en el servidor.' });
    if (!req.file) return res.status(400).json({ error: 'Falta la imagen.' });

    const prompt = String(req.body.prompt || '').trim();
    if (!prompt) return res.status(400).json({ error: 'Falta el prompt.' });

    const requestedDuration = Number(req.body.duration || 5);
    const duration = [5, 10].includes(requestedDuration) ? requestedDuration : 5;
    const ratio = ['1280:720','720:1280','960:960','1104:832','832:1104','1584:672','672:1584'].includes(req.body.ratio)
      ? req.body.ratio : '1280:720';

    const mime = req.file.mimetype || 'image/jpeg';
    const dataUri = `data:${mime};base64,${req.file.buffer.toString('base64')}`;

    const task = await client.imageToVideo.create({
      model: 'gen4.5',
      promptImage: dataUri,
      promptText: prompt,
      ratio,
      duration,
      outputFormat: 'mp4'
    });

    res.status(202).json({ taskId: task.id });
  } catch (error) {
    if (error instanceof TaskFailedError) return res.status(502).json({ error: 'Runway rechazó la generación.', details: error.taskDetails });
    console.error(error);
    res.status(500).json({ error: error?.message || 'Error generando el vídeo.' });
  }
});

app.get('/v1/tasks/:id', async (req, res) => {
  try {
    if (!client) return res.status(503).json({ error: 'Runway API no configurada.' });
    const task = await client.tasks.retrieve(req.params.id);
    res.json(task);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: error?.message || 'No se pudo consultar la tarea.' });
  }
});

app.listen(port, () => console.log(`JARVIS Video backend listening on ${port}`));
