import subprocess, time, re, json, os, shutil, sys, socket, struct
ROOT='/home/user/botpotmobaddon'; OUT='/tmp/claude-0/bench/results.json'
def pkt(i,t,b): d=struct.pack('<ii',i,t)+b.encode()+b'\0\0'; return struct.pack('<i',len(d))+d
def rd(s):
    n=struct.unpack('<i',s.recv(4))[0]; d=b''
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode()
class R:
    def __init__(s):
        s.s=socket.create_connection(('localhost',25575)); s.s.send(pkt(1,3,'test')); rd(s.s)
    def __call__(s,c): s.s.send(pkt(2,2,c)); return rd(s.s)
SP='{"apothic_spawners:ignore_players":1b,"apothic_spawners:ignore_conditions":1b,"apothic_spawners:ignore_light":1b}'
def grid(n):
    side=int(n**0.5); return [(x*8, z*8) for x in range(side) for z in range(side)]
def setup(r, kind, n):
    r('gamerule doDaylightCycle false'); r('time set midnight'); r('gamerule doMobSpawning false'); r('gamerule doWeatherCycle false')
    r('forceload add -16 -16 136 136')
    r('fill -16 -61 -16 136 -61 136 minecraft:stone_bricks')
    if kind=='baseline': return
    for x,z in grid(n):
        if kind.startswith('spawner'):
            r(f'setblock {x} -60 {z} minecraft:spawner{{SpawnData:{{entity:{{id:"minecraft:zombie"}}}},stats:{SP}}}')
        else:
            r(f'setblock {x} -60 {z} minecraft:chest')
            r(f'setblock {x} -59 {z} botanypots:terracotta_hopper_botany_pot')
            r(f'item replace block {x} -59 {z} container.0 with minecraft:soul_sand')
            r(f'item replace block {x} -59 {z} container.1 with minecraft:spawner[block_entity_data={{id:"minecraft:mob_spawner",SpawnData:{{entity:{{id:"minecraft:zombie"}}}}}}]')
    if kind=='spawner_kill':
        r('setblock -10 -55 -10 minecraft:repeating_command_block{auto:1b,Command:"kill @e[type=zombie]"}')
        r('setblock -9 -55 -10 minecraft:repeating_command_block{auto:1b,Command:"kill @e[type=item]"}')
def run(kind, n, warm=40, samples=12):
    shutil.rmtree(f'{ROOT}/run/world', ignore_errors=True)
    log=open('/tmp/claude-0/bench/server.log','w')
    p=subprocess.Popen(['./gradlew','runServer','-q'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
    for _ in range(120):
        time.sleep(3)
        if 'Done (' in open('/tmp/claude-0/bench/server.log').read(): break
    r=R(); setup(r,kind,n); time.sleep(warm)
    ms=[]
    for _ in range(samples):
        o=r('neoforge tps'); m=re.search(r'minecraft:overworld.*?([\d.]+) ?ms',o.replace('\n',' '),re.I) or re.search(r'Overworld.*?([\d.]+) ?ms',o,re.I)
        if m: ms.append(float(m.group(1)))
        time.sleep(5)
    ents=r('execute store result score #e x run kill @e[type=!player,type=!item]') if False else ''
    zc=r('execute if entity @e[type=zombie]'); ic=r('execute if entity @e[type=item]')
    chest=0
    if kind=='pots':
        for x,z in grid(n)[:16]:
            o=r(f'data get block {x} -60 {z} Items'); chest+=sum(int(c) for c in re.findall(r'count: (\d+)',o))
    r('stop'); p.wait(timeout=120)
    res=dict(kind=kind,n=n,ms=ms,zombies=zc,items=ic,chest16=chest,raw=o if not ms else '')
    print(json.dumps(res),flush=True)
    data=json.load(open(OUT)) if os.path.exists(OUT) else []
    data.append(res); json.dump(data,open(OUT,'w'),indent=1)
for spec in sys.argv[1:]:
    k,n=spec.split(':'); run(k,int(n))
