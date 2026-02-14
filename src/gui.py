
import os
import tkinter as tk
from tkinter import ttk, filedialog, messagebox
import threading
from src.parser import CNAB240Parser
from src.db import DatabaseManager
from src.comparator import Comparator

class Application(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Comparador CNAB240 Profissional")
        self.geometry("1100x850")
        
        # Configure Grid
        self.columnconfigure(0, weight=1)
        self.rowconfigure(0, weight=1)
        
        # Main Container
        main_frame = ttk.Frame(self, padding="10")
        main_frame.grid(row=0, column=0, sticky="nsew")
        main_frame.columnconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        main_frame.rowconfigure(1, weight=1) # Log area
        
        # --- Section 1: File Management ---
        file_frame = ttk.LabelFrame(main_frame, text="Gerenciamento de Arquivos", padding="10")
        file_frame.grid(row=0, column=0, columnspan=2, sticky="ew", padx=5, pady=5)
        file_frame.columnconfigure(0, weight=1)
        file_frame.columnconfigure(1, weight=1)
        
        # API Files Panel
        api_panel = ttk.Frame(file_frame)
        api_panel.grid(row=0, column=0, sticky="nsew", padx=5)
        ttk.Label(api_panel, text="Arquivos API:").pack(anchor="w")
        
        self.api_listbox = tk.Listbox(api_panel, height=8, selectmode=tk.EXTENDED)
        self.api_listbox.pack(fill="both", expand=True, pady=5)
        
        btn_api_frame = ttk.Frame(api_panel)
        btn_api_frame.pack(fill="x")
        ttk.Button(btn_api_frame, text="Adicionar Arquivos", command=self.add_api_files).pack(side="left", padx=2)
        ttk.Button(btn_api_frame, text="Limpar Lista", command=lambda: self.api_listbox.delete(0, tk.END)).pack(side="left", padx=2)
        
        # General Files Panel
        general_panel = ttk.Frame(file_frame)
        general_panel.grid(row=0, column=1, sticky="nsew", padx=5)
        ttk.Label(general_panel, text="Arquivos Geral:").pack(anchor="w")
        
        self.general_listbox = tk.Listbox(general_panel, height=8, selectmode=tk.EXTENDED)
        self.general_listbox.pack(fill="both", expand=True, pady=5)
        
        btn_gen_frame = ttk.Frame(general_panel)
        btn_gen_frame.pack(fill="x")
        ttk.Button(btn_gen_frame, text="Adicionar Arquivos", command=self.add_general_files).pack(side="left", padx=2)
        ttk.Button(btn_gen_frame, text="Limpar Lista", command=lambda: self.general_listbox.delete(0, tk.END)).pack(side="left", padx=2)
        
        # Action Buttons (Import)
        action_frame = ttk.Frame(file_frame)
        action_frame.grid(row=1, column=0, columnspan=2, pady=10)
        ttk.Button(action_frame, text="Importar para Banco de Dados", command=self.run_import).pack(side="left", padx=10)
        ttk.Button(action_frame, text="Limpar Banco de Dados", command=self.clear_db).pack(side="left", padx=10)
        
        # --- Section 2: Comparison & Reports ---
        compare_frame = ttk.LabelFrame(main_frame, text="Comparação e Relatórios", padding="10")
        compare_frame.grid(row=1, column=0, columnspan=2, sticky="ew", padx=5, pady=5)
        # Fix height by not letting it expand vertically too much, or setting a minsize if needed
        # Actually, packing the inner frame with fill=x and using grid sticky=ew is good.
        # To make it "fixed height" visually, we can just ensure the inner elements have consistent padding
        # and maybe set a minimum height for the row in grid if needed, but sticky=ew handles width.
        # For fixed height appearance, we can ensure row weight is 0 (default) so it doesn't expand.
        # It is already 0. The issue might be if window resizes, other elements might push it?
        # No, row 1 weight is 0. 
        # Let's add a fixed height frame container or just padding.
        
        # Filters and Buttons in the same row
        # Removed explicit height constraint to allow natural sizing
        filter_frame = ttk.Frame(compare_frame) 
        filter_frame.pack(fill="x", pady=15) # Increased external padding
        
        # Use grid instead of pack for better control
        # Create a container frame if needed, but grid directly into filter_frame is better
        # Or keep using pack side=left but with a larger container height implicitly
        
        container_h = ttk.Frame(filter_frame)
        container_h.pack(fill="x", expand=True)
        
        # Re-applying the controls with grid or pack with ipady
        # ipady (internal padding) is key here for buttons
        
        ttk.Label(container_h, text="Critério de Data:").pack(side="left", padx=5)
        self.date_criteria = tk.StringVar(value="data_credito")
        ttk.Radiobutton(container_h, text="Data Pagamento", variable=self.date_criteria, value="data_ocorrencia").pack(side="left", padx=5)
        ttk.Radiobutton(container_h, text="Data Crédito", variable=self.date_criteria, value="data_credito").pack(side="left", padx=5)
        
        ttk.Label(container_h, text=" | ").pack(side="left", padx=5)
        
        ttk.Label(container_h, text="Período (dd/mm/aaaa):").pack(side="left", padx=5)
        self.start_date_var = tk.StringVar()
        self.end_date_var = tk.StringVar()
        
        # ... (validation code) ...
        def validate_date(P):
            if len(P) == 0: return True
            if len(P) > 10: return False
            if not P[-1].isdigit() and P[-1] != '/': return False
            return True

        vcmd = (self.register(validate_date), '%P')
        
        # Improved Entry with placeholder/mask idea
        # Since tkinter doesn't have native mask, we use a binding to format input
        
        def format_date(event):
            entry = event.widget
            text = entry.get().replace('/', '')
            new_text = ""
            
            # Filter non-digits just in case
            text = ''.join(filter(str.isdigit, text))
            
            if len(text) > 0:
                new_text += text[:2]
            if len(text) >= 3:
                new_text += '/' + text[2:4]
            if len(text) >= 5:
                new_text += '/' + text[4:8]
                
            if new_text != entry.get():
                entry.delete(0, tk.END)
                entry.insert(0, new_text)
                entry.icursor(tk.END) # Move cursor to end

        self.start_entry = ttk.Entry(container_h, textvariable=self.start_date_var, width=12)
        self.start_entry.pack(side="left", padx=2)
        self.start_entry.bind('<KeyRelease>', format_date)
        
        ttk.Label(container_h, text="até").pack(side="left", padx=2)
        
        self.end_entry = ttk.Entry(container_h, textvariable=self.end_date_var, width=12)
        self.end_entry.pack(side="left", padx=2)
        self.end_entry.bind('<KeyRelease>', format_date)
        
        # Spacer
        ttk.Label(container_h, text="   ").pack(side="left", padx=10)
        
        # Buttons in the same row
        # Add explicit vertical padding to buttons to prevent cutting off text
        # Using a style with padding might also help, but pack pady is simpler.
        btn_exec = ttk.Button(container_h, text="Executar Comparação", command=self.run_comparison)
        btn_exec.pack(side="left", padx=5)
        
        btn_open = ttk.Button(container_h, text="Abrir Pasta de Relatórios", command=self.open_reports_folder)
        btn_open.pack(side="left", padx=5)
        
        # --- Section 3: Results Grid ---
        results_frame = ttk.LabelFrame(main_frame, text="Resultados da Comparação", padding="10")
        results_frame.grid(row=2, column=0, columnspan=2, sticky="nsew", padx=5, pady=5)
        main_frame.rowconfigure(2, weight=2) # Give more space to results
        
        # Notebook for different result tabs
        self.notebook = ttk.Notebook(results_frame)
        self.notebook.pack(fill="both", expand=True)
        
        # Trees for each category
        self.tree_discrepancies = self.create_treeview(self.notebook, "Divergências")
        self.tree_missing_api = self.create_treeview(self.notebook, "Faltando na API")
        self.tree_missing_geral = self.create_treeview(self.notebook, "Faltando no Geral")
        self.tree_matched = self.create_treeview(self.notebook, "Correspondentes")
        
        # --- Log Area ---
        log_frame = ttk.LabelFrame(main_frame, text="Log de Processamento", padding="5")
        log_frame.grid(row=3, column=0, columnspan=2, sticky="nsew", padx=5, pady=5)
        main_frame.rowconfigure(3, weight=1)
        
        self.log_text = tk.Text(log_frame, height=5, state='disabled')
        self.log_text.pack(fill="both", expand=True)
        
        # Scrollbar for log
        scrollbar = ttk.Scrollbar(log_frame, command=self.log_text.yview)
        scrollbar.pack(side="right", fill="y")
        self.log_text['yscrollcommand'] = scrollbar.set

    def create_treeview(self, parent, title):
        frame = ttk.Frame(parent)
        parent.add(frame, text=title)
        
        columns = ('nosso_numero', 'valor_pago', 'data_ocorrencia', 'status')
        tree = ttk.Treeview(frame, columns=columns, show='headings')
        
        tree.heading('nosso_numero', text='Nosso Número')
        tree.heading('valor_pago', text='Valor Pago')
        tree.heading('data_ocorrencia', text='Data')
        tree.heading('status', text='Status')
        
        tree.column('nosso_numero', width=150)
        tree.column('valor_pago', width=100)
        tree.column('data_ocorrencia', width=100)
        tree.column('status', width=150)
        
        # Scrollbars
        vsb = ttk.Scrollbar(frame, orient="vertical", command=tree.yview)
        hsb = ttk.Scrollbar(frame, orient="horizontal", command=tree.xview)
        tree.configure(yscrollcommand=vsb.set, xscrollcommand=hsb.set)
        
        tree.grid(column=0, row=0, sticky='nsew')
        vsb.grid(column=1, row=0, sticky='ns')
        hsb.grid(column=0, row=1, sticky='ew')
        
        frame.grid_columnconfigure(0, weight=1)
        frame.grid_rowconfigure(0, weight=1)
        
        return tree

    def populate_tree(self, tree, data, status_col=None, value_col='valor_pago', date_col='data_ocorrencia'):
        # Clear existing
        for item in tree.get_children():
            tree.delete(item)
            
        if not data: return

        for row in data:
            # Handle potential column name variations
            # If standard col not found, try to find with suffix or similar
            
            def get_val(r, col):
                if col in r: return r[col]
                if f"{col}_api" in r: return r[f"{col}_api"]
                if f"{col}_geral" in r: return r[f"{col}_geral"]
                return ""

            nosso_num = get_val(row, 'nosso_numero')
            val = get_val(row, value_col)
            date = get_val(row, date_col)
            status = row.get(status_col, "") if status_col else ""
            
            # Format value
            try:
                val = f"{float(val):,.2f}"
            except: pass
            
            tree.insert('', tk.END, values=(nosso_num, val, date, status))

    def log(self, message):
        self.log_text.config(state='normal')
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)
        self.log_text.config(state='disabled')
        self.update_idletasks()

    def add_api_files(self):
        files = filedialog.askopenfilenames(title="Selecione arquivos API", filetypes=[("Arquivos Retorno", "*.RET"), ("Todos", "*.*")])
        for f in files:
            self.api_listbox.insert(tk.END, f)

    def add_general_files(self):
        files = filedialog.askopenfilenames(title="Selecione arquivos Geral", filetypes=[("Arquivos Retorno", "*.RET"), ("Todos", "*.*")])
        for f in files:
            self.general_listbox.insert(tk.END, f)

    def clear_db(self):
        if messagebox.askyesno("Confirmar", "Tem certeza que deseja apagar TODOS os dados do banco?"):
            try:
                db = DatabaseManager()
                db.clear_tables()
                db.close()
                self.log("Banco de dados limpo com sucesso.")
                messagebox.showinfo("Sucesso", "Banco de dados limpo.")
            except Exception as e:
                self.log(f"Erro ao limpar banco: {e}")
                messagebox.showerror("Erro", str(e))

    def run_import(self):
        api_files = self.api_listbox.get(0, tk.END)
        gen_files = self.general_listbox.get(0, tk.END)
        
        if not api_files and not gen_files:
            messagebox.showwarning("Aviso", "Nenhum arquivo selecionado para importação.")
            return
            
        threading.Thread(target=self._import_process, args=(api_files, gen_files)).start()

    def _import_process(self, api_files, gen_files):
        try:
            db = DatabaseManager()
            parser = CNAB240Parser()
            
            if api_files:
                self.log(f"Iniciando importação de {len(api_files)} arquivos API...")
                all_transactions = []
                for f in api_files:
                    self.log(f"Lendo: {os.path.basename(f)}")
                    try:
                        t = parser.parse_file(f)
                        all_transactions.extend(t)
                    except Exception as e:
                        self.log(f"Erro ao ler {os.path.basename(f)}: {e}")
                
                if all_transactions:
                    self.log(f"Salvando {len(all_transactions)} registros API...")
                    db.save_transactions(all_transactions, 'api_transactions')
            
            if gen_files:
                self.log(f"Iniciando importação de {len(gen_files)} arquivos Geral...")
                all_transactions = []
                for f in gen_files:
                    self.log(f"Lendo: {os.path.basename(f)}")
                    try:
                        t = parser.parse_file(f)
                        all_transactions.extend(t)
                    except Exception as e:
                        self.log(f"Erro ao ler {os.path.basename(f)}: {e}")
                
                if all_transactions:
                    self.log(f"Salvando {len(all_transactions)} registros Geral...")
                    db.save_transactions(all_transactions, 'geral_transactions')
            
            db.close()
            self.log("Importação concluída!")
            messagebox.showinfo("Sucesso", "Importação finalizada com sucesso!")
            
        except Exception as e:
            self.log(f"Erro fatal na importação: {e}")
            messagebox.showerror("Erro", str(e))

    def run_comparison(self):
        threading.Thread(target=self._comparison_process).start()

    def _comparison_process(self):
        try:
            self.log("Iniciando comparação...")
            db = DatabaseManager()
            comp = Comparator(db)
            
            criteria = self.date_criteria.get()
            start = self.start_date_var.get().strip()
            end = self.end_date_var.get().strip()
            
            # Basic validation
            if (start and not end) or (end and not start):
                self.log("Aviso: Para filtrar por data, preencha Data Inicial E Final.")
                # We continue without filter or return? Let's continue without filter but warn
            
            results = comp.compare_transactions(date_field=criteria, start_date=start, end_date=end)
            
            if results:
                self.log("Gerando relatórios...")
                comp.generate_report(results)
                
                # Update GUI with results (must be done in main thread)
                self.after(0, lambda: self._update_results_ui(results, criteria))
                
                # Summary log
                self.log("--- Resumo ---")
                self.log(f"Faltando na API: {len(results['missing_in_api'])}")
                self.log(f"Faltando no Geral: {len(results['missing_in_geral'])}")
                self.log(f"Correspondentes: {len(results['matched'])}")
                self.log(f"Divergências: {len(results['discrepancies'])}")
                
                messagebox.showinfo("Sucesso", "Comparação realizada e relatórios gerados!")
            else:
                self.log("Nenhum resultado gerado (verifique se há dados ou se o filtro não excluiu tudo).")
                messagebox.showwarning("Aviso", "Sem resultados.")
                
            db.close()
            
        except Exception as e:
            self.log(f"Erro na comparação: {e}")
            import traceback
            self.log(traceback.format_exc())
            messagebox.showerror("Erro", str(e))

    def _update_results_ui(self, results, criteria):
        # Determine columns based on criteria
        val_col_api = 'valor_pago_api'
        val_col_geral = 'valor_pago_geral'
        date_col_api = f"{criteria}_api"
        date_col_geral = f"{criteria}_geral"
        
        # Populate Trees
        self.populate_tree(self.tree_discrepancies, results['discrepancies'], value_col=val_col_api, date_col=date_col_api)
        self.populate_tree(self.tree_missing_api, results['missing_in_api'], value_col=val_col_geral, date_col=date_col_geral)
        self.populate_tree(self.tree_missing_geral, results['missing_in_geral'], value_col=val_col_api, date_col=date_col_api)
        self.populate_tree(self.tree_matched, results['matched'], value_col=val_col_api, date_col=date_col_api)

    def open_reports_folder(self):
        path = os.path.abspath("reports")
        if not os.path.exists(path):
            os.makedirs(path)
        os.startfile(path)

if __name__ == "__main__":
    app = Application()
    app.mainloop()
