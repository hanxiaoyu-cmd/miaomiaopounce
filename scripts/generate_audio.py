"""Generate original, soft feedback sounds. No downloaded assets or dependencies."""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
OUT.mkdir(parents=True, exist_ok=True)
rng = random.Random(2048)
for name, duration in [('bug', .17), ('fish', .23), ('mouse', .16), ('dot', .19)]:
    samples = []
    phase = 0.0
    for i in range(int(RATE * duration)):
        t = i / RATE
        progress = t / duration
        envelope = math.sin(math.pi * progress) ** 2 * math.exp(-progress * 2)
        if name == 'bug':
            frequency = 850 - 420 * progress
            signal = math.sin(phase) + .15 * math.sin(phase * 2)
        elif name == 'fish':
            frequency = 380 + 400 * math.exp(-progress * 8)
            signal = math.sin(phase)
        elif name == 'mouse':
            frequency = 650 - 180 * progress
            signal = .45 * math.sin(phase) + .35 * rng.uniform(-1, 1)
        else:
            frequency = 570 - 270 * progress
            signal = math.sin(phase)
        phase += 2 * math.pi * frequency / RATE
        samples.append(int(32767 * .32 * envelope * signal))
    with wave.open(str(OUT / f'{name}.wav'), 'wb') as audio:
        audio.setnchannels(1); audio.setsampwidth(2); audio.setframerate(RATE)
        audio.writeframes(struct.pack('<' + 'h' * len(samples), *samples))
print('Generated 4 original mono feedback sounds.')
