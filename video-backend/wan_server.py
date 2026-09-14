from flask import Flask, request, jsonify
import os, subprocess, uuid

app = Flask(__name__)

@app.get('/health')
def health():
    return jsonify(ok=True, provider='Wan2.1', mode='local-open-source')

@app.post('/v1/video')
def video():
    if 'image' not in request.files:
        return jsonify(error='image is required'), 400
    prompt = request.form.get('prompt','').strip()
    if not prompt:
        return jsonify(error='prompt is required'), 400
    # This endpoint is intentionally a local-model adapter. The machine running it
    # must have Wan2.1 installed and a GPU with sufficient VRAM.
    job = str(uuid.uuid4())
    os.makedirs('/tmp/jarvis', exist_ok=True)
    image_path = f'/tmp/jarvis/{job}.jpg'
    request.files['image'].save(image_path)
    return jsonify(status='accepted', jobId=job, message='Wan2.1 local generation is ready; run the model worker on this host.')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.getenv('PORT', '7860')))
