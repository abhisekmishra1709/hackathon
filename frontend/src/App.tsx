import { FormEvent, useEffect, useState } from "react";
import { Activity, BellRing, BriefcaseBusiness, Database, FileUp, LogOut, ShieldCheck, X } from "lucide-react";

type Credentials = { username: string; password: string };
type ImportKind = "customers" | "accounts" | "transactions";
type ImportResult = { created: number; errors: string[] };
type Alert = { id: number; customer: string; accountId: string; rules: string; riskScore: number; status: string; evidenceCount: number; createdAt: string };
type AlertDetail = Alert & { customerId: string; customerName: string; explanation: string; transactionIds: string[] };
type Case = { id: number; alertId: number; status: string; analystId: string; createdAt: string };
const importOrder: ImportKind[] = ["customers", "accounts", "transactions"];

function App() {
  const [serviceStatus, setServiceStatus] = useState("Connecting");
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [cases, setCases] = useState<Case[]>([]);
  const [selectedAlert, setSelectedAlert] = useState<AlertDetail | null>(null);
  const [files, setFiles] = useState<Partial<Record<ImportKind, File>>>({});
  const [notice, setNotice] = useState("Sign in to begin monitoring.");
  const [activeView, setActiveView] = useState<"overview" | "alerts" | "cases" | "data">("overview");

  useEffect(() => {
    fetch("/api/v1/system/status").then((response) => response.json())
      .then((data: { status: string }) => setServiceStatus(data.status)).catch(() => setServiceStatus("Offline"));
  }, []);

  async function api(path: string, options: RequestInit = {}, auth = credentials) {
    const headers = new Headers(options.headers);
    if (auth) headers.set("Authorization", `Basic ${btoa(`${auth.username}:${auth.password}`)}`);
    if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
    const response = await fetch(path, { ...options, headers });
    if (!response.ok) {
      const body = await response.json().catch(() => null) as { message?: string } | null;
      throw new Error(body?.message ?? `Request failed (${response.status})`);
    }
    return response;
  }

  async function refresh(auth = credentials) {
    if (!auth) return;
    const [alertResponse, caseResponse] = await Promise.all([api("/api/v1/alerts", {}, auth), api("/api/v1/cases", {}, auth)]);
    setAlerts(await alertResponse.json() as Alert[]);
    setCases(await caseResponse.json() as Case[]);
  }

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const next = { username: String(form.get("username")), password: String(form.get("password")) };
    try {
      await refresh(next);
      setCredentials(next);
      setNotice("Signed in. Import customers, accounts, then transactions.");
    } catch { setNotice("Sign-in failed. Check the development credentials."); }
  }

  async function importFiles() {
    if (!credentials || importOrder.some((kind) => !files[kind])) {
      setNotice("Choose customers.csv, accounts.csv, and transactions.csv first."); return;
    }
    try {
      const results: string[] = [];
      for (const kind of importOrder) {
        const form = new FormData(); form.append("file", files[kind]!);
        const response = await api(`/api/v1/ingestion/${kind}/csv`, { method: "POST", body: form });
        const result = await response.json() as ImportResult;
        results.push(`${kind}: ${result.created} imported, ${result.errors.length} rejected`);
      }
      setNotice(results.join(" · ")); await refresh(); setActiveView("alerts");
    } catch (error) { setNotice(error instanceof Error ? error.message : "Import failed"); }
  }

  async function openAlert(id: number) {
    const response = await api(`/api/v1/alerts/${id}`);
    setSelectedAlert(await response.json() as AlertDetail);
  }

  async function createCase(alertId: number) {
    if (!credentials) return;
    await api(`/api/v1/cases/from-alert/${alertId}`, { method: "POST", body: JSON.stringify({ notes: "Opened from analyst dashboard" }) });
    setSelectedAlert(null); await refresh(); setActiveView("cases");
  }

  async function closeCase(caseId: number) {
    await api(`/api/v1/cases/${caseId}`, { method: "PATCH", body: JSON.stringify({ status: "CLOSED", notes: "Review completed", disposition: "Escalated for regulatory review" }) });
    await refresh();
  }

  const openAlerts = alerts.filter((alert) => alert.status !== "CLOSED");
  const highRisk = openAlerts.filter((alert) => alert.riskScore >= 80);

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><ShieldCheck size={28} strokeWidth={1.8} /><span>Sentinel</span></div>
      <nav aria-label="Primary navigation">{([
        ["overview", Activity, "Overview"], ["alerts", BellRing, "Alerts"], ["cases", BriefcaseBusiness, "Cases"], ["data", Database, "Data ingestion"],
      ] as const).map(([view, Icon, label]) => <button className={activeView === view ? "active" : ""} onClick={() => setActiveView(view)} key={view}><Icon size={18} />{label}</button>)}</nav>
      <div className="sidebar-foot"><div className="service-state"><span className={`status-dot ${serviceStatus.toLowerCase()}`} />API {serviceStatus}</div>{credentials && <button className="logout" onClick={() => setCredentials(null)}><LogOut size={16} />Sign out</button>}</div>
    </aside>
    <main>
      <header><div><p className="eyebrow">MERIDIANTRUST / COMPLIANCE</p><h1>Risk operations</h1></div><div className="analyst">{credentials?.username.slice(0, 2).toUpperCase() ?? "--"}</div></header>
      {!credentials ? <section className="login-panel"><p className="eyebrow">SECURE ACCESS</p><h2>Analyst sign in</h2><form onSubmit={login}><label>Username<input name="username" autoComplete="username" required /></label><label>Password<input name="password" type="password" autoComplete="current-password" required /></label><button type="submit">Sign in</button></form><p className="notice">{notice}</p></section> : <>
        {(activeView === "overview" || activeView === "alerts") && <><section className="metrics" aria-label="Alert summary"><article className="critical"><p>Open alerts</p><strong>{openAlerts.length}</strong><small>Awaiting analyst review</small></article><article><p>Active cases</p><strong>{cases.filter((item) => item.status !== "CLOSED").length}</strong><small>Investigations assigned</small></article><article className="warning"><p>High risk</p><strong>{highRisk.length}</strong><small>Risk score 80 or above</small></article></section><section className="queue"><div className="section-heading"><div><p className="eyebrow">PRIORITY QUEUE</p><h2>Alerts requiring review</h2></div><button onClick={() => setActiveView("data")}>Import data</button></div>{alerts.length === 0 ? <EmptyState /> : <div className="table-wrap"><table><thead><tr><th>Risk</th><th>Customer</th><th>Account</th><th>Rules</th><th>Status</th></tr></thead><tbody>{alerts.map((alert) => <tr key={alert.id} onClick={() => openAlert(alert.id)}><td><b className={`risk risk-${alert.riskScore >= 80 ? "high" : "medium"}`}>{alert.riskScore}</b></td><td>{alert.customer}</td><td>{alert.accountId}</td><td>{alert.rules.replaceAll(",", " · ")}</td><td>{alert.status}</td></tr>)}</tbody></table></div>}</section></>}
        {activeView === "data" && <section className="workspace-panel"><p className="eyebrow">CONTROLLED INGESTION</p><h2>Import monitoring data</h2><p className="supporting">Upload reference data first, then transactions. Invalid rows are reported without hiding successful records.</p><div className="upload-grid">{importOrder.map((kind, index) => <label className="upload" key={kind}><span>{index + 1}</span><FileUp size={22} /><b>{kind}.csv</b><small>{files[kind]?.name ?? "Choose CSV file"}</small><input type="file" accept=".csv,text/csv" onChange={(event) => setFiles((current) => ({ ...current, [kind]: event.target.files?.[0] }))} /></label>)}</div><button onClick={importFiles}>Import and run detection</button><p className="notice">{notice}</p></section>}
        {activeView === "cases" && <section className="workspace-panel"><p className="eyebrow">INVESTIGATIONS</p><h2>Case register</h2>{cases.length === 0 ? <p className="notice">No cases have been created.</p> : cases.map((item) => <article className="case-row" key={item.id}><div><b>Case #{item.id}</b><span>Alert #{item.alertId} · {item.analystId}</span></div><strong>{item.status}</strong>{item.status !== "CLOSED" && <button onClick={() => closeCase(item.id)}>Close and escalate</button>}</article>)}</section>}
      </>}
    </main>
    {selectedAlert && <div className="drawer-backdrop" onClick={() => setSelectedAlert(null)}><aside className="drawer" onClick={(event) => event.stopPropagation()}><button className="icon-button" aria-label="Close details" onClick={() => setSelectedAlert(null)}><X /></button><p className="eyebrow">ALERT #{selectedAlert.id}</p><div className="score">{selectedAlert.riskScore}</div><h2>{selectedAlert.rules.replaceAll(",", " · ")}</h2><p>{selectedAlert.explanation}</p><dl><dt>Customer</dt><dd>{selectedAlert.customerName} ({selectedAlert.customerId})</dd><dt>Account</dt><dd>{selectedAlert.accountId}</dd><dt>Evidence</dt><dd>{selectedAlert.transactionIds.join(", ")}</dd></dl>{selectedAlert.status === "OPEN" && <button onClick={() => createCase(selectedAlert.id)}>Create investigation case</button>}</aside></div>}
  </div>;
}

function EmptyState() { return <div className="empty-state"><ShieldCheck size={38} strokeWidth={1.4} /><h3>No alerts detected</h3><p>Import customer, account, and transaction data to begin monitoring.</p></div>; }
export default App;