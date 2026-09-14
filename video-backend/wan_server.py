from flask import Flask, request, jsonify, send_from_directory
import os, subprocess, threading, uuid, time, shutil

app = Flask(__name__)
ROOT = os.environ.get('WAN_ROOT', os.path.abspath('../Wan2.2'))
CKPT = os.environ.get('WAN_CKPT', os.path.join(ROOT, 'Wan2.2-TI2V-5B'))
GEN = os.environ.get('WAN_GENERATE', os.path.join(ROOT, 'generate.py'))
OUT = os.path.abspath(os.environ.get('JARVIS_OUTPUT', './outputs'))
PORT = int(os.environ.get('PORT', '7860'))
os.makedirs(OUT, exist_ok=True)
JOBS = {}

def run_job(job_id, image_path, prompt, duration):
    job_dir = os.path.join(OUT, job_id)
    os.makedirs(job_dir, exist_ok=True)
    JOBS[job_id]['status'] = 'running'
    JOBS[job_id]['started'] = time.time()
    cmd = ['python', GEN, '--task', 'ti2v-5B', '--size', '1280*704', '--ckpt_dir', CKPT,
           '--offload_model', 'True', '--convert_model_dtype', '--t5_cpu',
           '--image', image_path, '--prompt', prompt]
    try:
        result = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
        if result.returncode != 0:
            JOBS[job_id].update(status='failed', error=(result.stderr or result.stdout)[-4000:])
            return
        candidates = []
        for base in (ROOT, job_dir):
            for dp, _, fs in os.walk(base):
                candidates += [os.path.join(dp, f) for f in fs if f.endswith('.mp4')]
        if not candidates:
            JOBS[job_id].update(status='failed', error='Wan terminó sin producir un MP4.')
            return
        newest = max(candidates, key=os.path.getmtime)
        output = os.path.join(job_dir, 'video.mp4')
        if os.path.abspath(newest) != os.path.abspath(output): shutil.copy2(newest, output)
        JOBS[job_id].update(status='completed', url=f'/v1/jobs/{job_id}/video', finished=time.time())
    except Exception as exc:
        JOBS[job_id].update(status='failed', error=str(exc))

@app.get('/health')
def health():
    return jsonify(ok=True, provider='Wan2.2-TI2V-5B', free_local=True,
                    model_exists=os.path.isdir(CKPT), generator_exists=os.path.isfile(GEN))

@app.post('/v1/video')
def video():
    if 'image' not in request.files: return jsonify(error='Falta la imagen.'), 400
    prompt = request.form.get('prompt', '').strip()
    if not prompt: return jsonify(error='Falta el prompt.'), 400
    try: duration = int(request.form.get('duration', '5'))
    except ValueError: duration = 5
    if duration not in (5, 10, 15): return jsonify(error='Duración permitida: 5, 10 o 15 segundos.'), 400
    if not os.path.isfile(GEN) or not os.path.isdir(CKPT):
        return jsonify(error='Wan2.2 no está instalado/configurado en este equipo.'), 503
    job_id = str(uuid.uuid4()); job_dir = os.path.join(OUT, job_id); os.makedirs(job_dir, exist_ok=True)
    image_path = os.path.join(job_dir, 'input.jpg'); request.files['image'].save(image_path)
    JOBS[job_id] = {'status':'queued', 'created':time.time()}
    threading.Thread(target=run_job, args=(job_id, image_path, prompt, duration), daemon=True).start()
    return jsonify(status='queued', jobId=job_id), 202

@app.get('/v1/jobs/<job_id>')
def job_status(job_id):
    job = JOBS.get(job_id)
    if not job: return jsonify(error='Trabajo no encontrado.'), 404
    return jsonify(job)

@app.get('/v1/jobs/<job_id>/video')
def job_video(job_id):
    path = os.path.join(OUT, job_id, 'video.mp4')
    if not os.path.isfile(path): return jsonify(error='Vídeo todavía no disponible.'), 404
    return send_from_directory(os.path.dirname(path), 'video.mp4', mimetype='video/mp4')

if __name__ == '__main__': app.run(host='0.0.0.0', port=PORT, threaded=True)
