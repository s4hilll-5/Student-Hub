import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

// A fresh mock per test (assigned in beforeEach); the context forwards to whichever is current.
let login;
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ login: (...args) => login(...args) }) }));
vi.mock('../context/ThemeContext', () => ({ useTheme: () => ({ theme: 'light', toggleTheme: () => {} }) }));

const { default: Login } = await import('../pages/auth/Login');

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<p>Dashboard page</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('Login page', () => {
  beforeEach(() => {
    login = vi.fn();
  });

  it('validates required fields before calling the API', async () => {
    const user = userEvent.setup();
    renderLogin();
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(screen.getByText('Enter your email')).toBeInTheDocument();
    expect(screen.getByText('Enter your password')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('signs in and goes to the dashboard', async () => {
    const user = userEvent.setup();
    login.mockResolvedValue({ fullName: 'Asha Rao' });
    renderLogin();
    await user.type(screen.getByLabelText(/Email/), 'asha@example.com');
    await user.type(screen.getByLabelText(/^Password/), 'secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(login).toHaveBeenCalledWith('asha@example.com', 'secret123');
    expect(await screen.findByText('Dashboard page')).toBeInTheDocument();
  });

  it("shows the server's message when sign-in fails", async () => {
    const user = userEvent.setup();
    const apiError = Object.assign(new Error('Request failed with status code 401'), {
      response: { status: 401, data: { message: 'Invalid email or password.' } },
    });
    login.mockImplementation(async () => {
      throw apiError;
    });
    renderLogin();
    await user.type(screen.getByLabelText(/Email/), 'asha@example.com');
    await user.type(screen.getByLabelText(/^Password/), 'wrongpass1');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.');
  });
});
