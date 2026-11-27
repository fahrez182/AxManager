const AxDark={async exec(c){try{if(window.ksu&&ksu.exec)return await ksu.exec(c)}catch(e){}try{if(window.Axeron&&Axeron.exec)return await window.Axeron.exec(c)}catch(e){}return "[AxDarkGov2-Preview] "+c}};
window.AxDark=AxDark;
