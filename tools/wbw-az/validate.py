"""Agent çıxışını yoxlayır: python3 validate.py <hissə> [--partial] — bax GUIDE.md."""
import json,glob,re,sys,os
# İş qovluğu (in/, out/): WBW_WORK, yoxdursa skriptin öz qovluğu. Hissə adı: NN və ya `stale`.
H=os.environ.get('WBW_WORK') or os.path.dirname(os.path.abspath(__file__))
def expected(nn):
    t=open(f'{H}/in/{nn}.txt').read(); return re.findall(r'^### (\d+:\d+) \[n=(\d+)\]',t,re.M)
BAD=re.compile(r"[؀-ۿ]|[âîûêôÂÎÛ]|\w'\w")
def check(nn,final=True):
    got={}
    for f in sorted(glob.glob(f'{H}/out/{nn}_*.json')):
        try: d=json.load(open(f))
        except Exception as e: print('JSON XƏTASI',f,e); continue
        got.update(d)
    errs=[]
    exp=expected(nn)
    for k,n in exp:
        if k not in got:
            if final: errs.append(f'{k}: yoxdur')
            continue
        v=got[k]
        if not isinstance(v,list) or len(v)!=int(n): errs.append(f'{k}: {len(v) if isinstance(v,list) else "?"} element, gözlənilən {n}')
        for i,s in enumerate(v if isinstance(v,list) else []):
            if not isinstance(s,str) or not s.strip(): errs.append(f'{k}[{i}]: boş')
            elif BAD.search(s): errs.append(f'{k}[{i}]: şübhəli simvol: {s}')
            elif s.count('(')!=s.count(')'): errs.append(f'{k}[{i}]: mötərizə balansı: {s}')
    extra=set(got)-{k for k,_ in exp}
    for k in extra: errs.append(f'{k}: bu hissəyə aid deyil')
    done=sum(1 for k,_ in exp if k in got)
    print(f'hissə {nn}: {done}/{len(exp)} ayə, {len(errs)} xəta')
    for e in errs[:60]: print('  ',e)
    return errs
if __name__=='__main__':
    check(sys.argv[1], final='--partial' not in sys.argv)
